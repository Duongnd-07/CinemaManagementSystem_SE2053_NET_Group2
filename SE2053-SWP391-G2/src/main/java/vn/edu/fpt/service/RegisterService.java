package vn.edu.fpt.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.EmailVerificationDAO;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.EmailVerification;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.AppConfig;
import vn.edu.fpt.util.EmailService;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-01: Nghiệp vụ đăng ký tài khoản thành viên qua xác thực Email (OTP)
public class RegisterService {
    // SRS UC-01: Full Name max length 50 characters
    private static final int MAX_FULLNAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 32;

    // Pattern kiểm tra email hợp lệ chuẩn RFC 5322 cơ bản
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    // Pattern số điện thoại Việt Nam: 10 chữ số bắt đầu bằng số 0
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    // Pattern mật khẩu: ít nhất 1 chữ thường, 1 chữ hoa, 1 số
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserDAO userDAO;
    private final EmailVerificationDAO emailVerificationDAO;

    public RegisterService() {
        this(new UserDAO(), new EmailVerificationDAO());
    }

    public RegisterService(UserDAO userDAO, EmailVerificationDAO emailVerificationDAO) {
        this.userDAO = userDAO;
        this.emailVerificationDAO = emailVerificationDAO;
    }

    /**
     * Validate dữ liệu form đăng ký. Trả về Map chứa lỗi theo từng trường (rỗng = hợp lệ).
     */
    public Map<String, String> validate(RegisterForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Họ và tên
        String fullName = form.getFullName();
        if (fullName.isEmpty()) {
            errors.put("fullName", Messages.format(Messages.MSG02, "Họ và tên"));
        } else if (fullName.length() > MAX_FULLNAME) {
            errors.put("fullName", Messages.format(Messages.MSG08, MAX_FULLNAME));
        }

        // 2. Email
        String email = form.getEmail();
        if (email.isEmpty()) {
            errors.put("email", Messages.format(Messages.MSG02, "Email"));
        } else if (email.length() > MAX_EMAIL) {
            errors.put("email", Messages.format(Messages.MSG08, MAX_EMAIL));
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", Messages.REGISTER_EMAIL_INVALID);
        } else if (userDAO.existsByEmail(email)) {
            errors.put("email", Messages.REGISTER_EMAIL_DUPLICATE);
        }

        // 3. Số điện thoại
        String phone = form.getPhone();
        if (phone.isEmpty()) {
            errors.put("phone", Messages.format(Messages.MSG02, "Số điện thoại"));
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put("phone", Messages.REGISTER_PHONE_INVALID);
        } else if (userDAO.existsByPhone(phone)) {
            errors.put("phone", Messages.REGISTER_PHONE_DUPLICATE);
        }

        // 4. Mật khẩu
        String password = form.getPassword();
        if (password.isEmpty()) {
            errors.put("password", Messages.format(Messages.MSG02, "Mật khẩu"));
        } else if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            errors.put("password", Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD));
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errors.put("password", Messages.REGISTER_PASSWORD_WEAK);
        }

        // 5. Xác nhận mật khẩu
        String confirmPassword = form.getConfirmPassword();
        if (confirmPassword.isEmpty()) {
            errors.put("confirmPassword", Messages.format(Messages.MSG02, "Xác nhận mật khẩu"));
        } else if (!password.equals(confirmPassword)) {
            errors.put("confirmPassword", Messages.REGISTER_PASSWORD_MISMATCH);
        }

        // 6. Điều khoản & Chính sách (SRS UC-01 Agreement checkbox)
        if (!form.isAgreedTerms()) {
            errors.put("agreedTerms", Messages.REGISTER_TERMS_REQUIRED);
        }

        return errors;
    }

    /**
     * Bắt đầu quy trình đăng ký: Validate dữ liệu, sinh mã OTP và gửi email cho người dùng.
     *
     * @return Mã OTP đã được sinh
     */
    public String initiateRegistration(RegisterForm form) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        int expiryMinutes = AppConfig.getInt("mail.otp.expiryMinutes", 10);
        String otpCode = generateOtpCode();

        // Vô hiệu hóa các mã OTP trước đó của email này
        emailVerificationDAO.invalidatePreviousOtps(form.getEmail());

        // Lưu mã OTP mới vào database
        EmailVerification ev = new EmailVerification();
        ev.setEmail(form.getEmail());
        ev.setOtpCode(otpCode);
        ev.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        ev.setUsed(false);
        ev.setCreatedAt(LocalDateTime.now());
        emailVerificationDAO.insert(ev);

        // Gửi email bất đồng bộ kèm mã OTP
        EmailService.sendRegistrationOtpAsync(form.getEmail(), form.getFullName(), otpCode, expiryMinutes);

        return otpCode;
    }

    /**
     * Gửi lại mã OTP mới khi mã cũ hết hạn hoặc người dùng yêu cầu gửi lại.
     */
    public String resendOtp(RegisterForm form) throws SQLException {
        int expiryMinutes = AppConfig.getInt("mail.otp.expiryMinutes", 10);
        String otpCode = generateOtpCode();

        emailVerificationDAO.invalidatePreviousOtps(form.getEmail());

        EmailVerification ev = new EmailVerification();
        ev.setEmail(form.getEmail());
        ev.setOtpCode(otpCode);
        ev.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        ev.setUsed(false);
        ev.setCreatedAt(LocalDateTime.now());
        emailVerificationDAO.insert(ev);

        EmailService.sendRegistrationOtpAsync(form.getEmail(), form.getFullName(), otpCode, expiryMinutes);

        return otpCode;
    }

    /**
     * Xác minh mã OTP người dùng nhập và hoàn tất lưu User vào CSDL.
     *
     * @return User vừa được tạo thành công
     */
    public User verifyOtpAndComplete(RegisterForm form, String inputOtp) throws SQLException, ValidationException {
        String otp = inputOtp == null ? "" : inputOtp.trim();
        if (otp.length() != 6 || !otp.chars().allMatch(Character::isDigit)) {
            throw new ValidationException(Map.of("otp", Messages.REGISTER_OTP_INVALID));
        }

        // Kiểm tra mã OTP trong DB
        EmailVerification ev = emailVerificationDAO.findLatestValidOtp(form.getEmail(), otp);
        if (ev == null) {
            throw new ValidationException(Map.of("otp", Messages.REGISTER_OTP_INVALID));
        }

        // Kiểm tra lại tránh race condition (email hoặc phone đã bị người khác đăng ký trong lúc chờ nhập OTP)
        if (userDAO.existsByEmail(form.getEmail())) {
            throw new ValidationException(Map.of("general", Messages.REGISTER_EMAIL_DUPLICATE));
        }
        if (userDAO.existsByPhone(form.getPhone())) {
            throw new ValidationException(Map.of("general", Messages.REGISTER_PHONE_DUPLICATE));
        }

        // Băm mật khẩu an toàn bằng PBKDF2 + salt
        String passwordHash = PasswordUtil.hash(form.getPassword());

        // Tạo User mới theo thiết kế
        User user = new User();
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setPasswordHash(passwordHash);
        user.setRole(Role.CUSTOMER);
        user.setPointBalance(0);
        user.setStatus("Active");

        userDAO.insert(user);

        // Đánh dấu mã OTP đã được sử dụng thành công
        emailVerificationDAO.markUsed(ev.getVerificationId());

        return user;
    }

    private String generateOtpCode() {
        // Sinh ngẫu nhiên 6 chữ số từ 100000 đến 999999
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
