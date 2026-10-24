package vn.edu.fpt.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.EmailVerificationDAO;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.EmailVerification;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.AppConfig;
import vn.edu.fpt.util.EmailService;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-10: Đổi email đăng nhập. Email mới chỉ được lưu sau khi chủ tài khoản nhập đúng OTP gửi tới email đó.
public class EmailChangeService {
    public static final String ERROR_PENDING = "pending";
    private static final int MAX_EMAIL = 100;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    // Cùng pattern với UC-01 để email đăng ký và email đổi mới được kiểm tra như nhau
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{6}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDAO userDAO;
    private final EmailVerificationDAO emailVerificationDAO;

    public EmailChangeService() {
        this(new UserDAO(), new EmailVerificationDAO());
    }

    public EmailChangeService(UserDAO userDAO, EmailVerificationDAO emailVerificationDAO) {
        this.userDAO = userDAO;
        this.emailVerificationDAO = emailVerificationDAO;
    }

    /**
     * Bước 1: kiểm tra email mới và mật khẩu hiện tại (BR-PROF-04), rồi gửi OTP tới email mới.
     */
    public PendingEmailChange requestChange(int userId, String rawNewEmail, String currentPassword)
            throws SQLException, ValidationException {
        User user = requireUser(userId);
        String newEmail = rawNewEmail == null ? "" : rawNewEmail.trim().toLowerCase(Locale.ROOT);
        String password = currentPassword == null ? "" : currentPassword;
        Map<String, String> errors = new LinkedHashMap<>();

        if (newEmail.isEmpty()) {
            errors.put("newEmail", Messages.format(Messages.MSG02, "Email mới"));
        } else if (newEmail.length() > MAX_EMAIL) {
            errors.put("newEmail", Messages.format(Messages.MSG08, MAX_EMAIL));
        } else if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
            errors.put("newEmail", Messages.REGISTER_EMAIL_INVALID);
        } else if (newEmail.equalsIgnoreCase(user.getEmail())) {
            errors.put("newEmail", Messages.EMAIL_CHANGE_SAME);
        } else if (userDAO.existsByEmail(newEmail)) {
            errors.put("newEmail", Messages.REGISTER_EMAIL_DUPLICATE);
        }

        if (password.isEmpty()) {
            errors.put("emailPassword", Messages.format(Messages.MSG02, "Mật khẩu hiện tại"));
        } else if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            errors.put("emailPassword", Messages.PROFILE_CURRENT_PASSWORD_WRONG);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        sendOtp(user, newEmail);
        return new PendingEmailChange(userId, newEmail, LocalDateTime.now());
    }

    /**
     * Gửi lại OTP, giới hạn 1 lần mỗi RESEND_COOLDOWN_SECONDS để không bị dùng làm công cụ spam email.
     */
    public void resendOtp(int userId, PendingEmailChange pending) throws SQLException, ValidationException {
        User user = requirePending(userId, pending);
        long waitSeconds = RESEND_COOLDOWN_SECONDS
                - Duration.between(pending.getLastSentAt(), LocalDateTime.now()).toSeconds();
        if (waitSeconds > 0) {
            throw new ValidationException(Map.of("otp", Messages.format(Messages.EMAIL_CHANGE_RESEND_WAIT, waitSeconds)));
        }
        if (userDAO.existsByEmail(pending.getNewEmail())) {
            throw new ValidationException(Map.of(ERROR_PENDING, Messages.REGISTER_EMAIL_DUPLICATE));
        }
        sendOtp(user, pending.getNewEmail());
        pending.setLastSentAt(LocalDateTime.now());
        pending.setFailedAttempts(0);
    }

    /**
     * Bước 2: xác thực OTP rồi lưu email mới. Trả về User mới nhất để controller làm mới session.
     */
    public User confirmChange(int userId, PendingEmailChange pending, String rawOtp)
            throws SQLException, ValidationException {
        User user = requirePending(userId, pending);
        if (pending.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
            throw tooManyAttempts();
        }

        String otp = rawOtp == null ? "" : rawOtp.trim();
        if (otp.isEmpty()) {
            throw new ValidationException(Map.of("otp", Messages.format(Messages.MSG02, "Mã xác thực")));
        }
        EmailVerification verification = OTP_PATTERN.matcher(otp).matches()
                ? emailVerificationDAO.findLatestValidOtp(pending.getNewEmail(), otp)
                : null;
        if (verification == null) {
            // Đếm số lần sai để chặn dò mã 6 chữ số
            pending.setFailedAttempts(pending.getFailedAttempts() + 1);
            if (pending.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
                throw tooManyAttempts();
            }
            throw new ValidationException(Map.of("otp", Messages.REGISTER_OTP_INVALID));
        }

        // Email có thể đã bị người khác đăng ký trong lúc chờ nhập OTP
        if (userDAO.existsByEmail(pending.getNewEmail())) {
            throw new ValidationException(Map.of(ERROR_PENDING, Messages.REGISTER_EMAIL_DUPLICATE));
        }
        try {
            userDAO.updateEmail(user.getUserId(), pending.getNewEmail());
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of(ERROR_PENDING, Messages.REGISTER_EMAIL_DUPLICATE));
            }
            throw e;
        }
        emailVerificationDAO.markUsed(verification.getVerificationId());
        EmailService.sendEmailChangedNoticeAsync(user.getEmail(), user.getFullName(), pending.getNewEmail());
        return requireUser(userId);
    }

    private void sendOtp(User user, String newEmail) throws SQLException {
        int expiryMinutes = AppConfig.getInt("mail.otp.expiryMinutes", 10);
        String otpCode = String.valueOf(100000 + RANDOM.nextInt(900000));

        emailVerificationDAO.invalidatePreviousOtps(newEmail);
        EmailVerification verification = new EmailVerification();
        verification.setEmail(newEmail);
        verification.setOtpCode(otpCode);
        verification.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        verification.setUsed(false);
        verification.setCreatedAt(LocalDateTime.now());
        emailVerificationDAO.insert(verification);

        EmailService.sendEmailChangeOtpAsync(newEmail, user.getFullName(), otpCode, expiryMinutes);
    }

    private User requirePending(int userId, PendingEmailChange pending) throws SQLException, ValidationException {
        if (pending == null || pending.getUserId() != userId) {
            throw new ValidationException(Map.of(ERROR_PENDING, Messages.EMAIL_CHANGE_NO_PENDING));
        }
        return requireUser(userId);
    }

    private User requireUser(int userId) throws SQLException, ValidationException {
        User user = userDAO.findProfileById(userId);
        if (user == null) {
            throw new ValidationException(Map.of("general", Messages.PROFILE_NOT_FOUND));
        }
        return user;
    }

    private ValidationException tooManyAttempts() {
        return new ValidationException(Map.of(ERROR_PENDING,
                Messages.format(Messages.EMAIL_CHANGE_TOO_MANY_ATTEMPTS, MAX_OTP_ATTEMPTS)));
    }
}
