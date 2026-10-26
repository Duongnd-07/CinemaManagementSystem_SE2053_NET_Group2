package vn.edu.fpt.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.PasswordResetDAO;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.PasswordResetToken;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.AppConfig;
import vn.edu.fpt.util.EmailService;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-03: Nghiệp vụ Quên mật khẩu & Đặt lại mật khẩu mới qua liên kết gửi về email
public class PasswordResetService {
    private static final int MAX_EMAIL = 100;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 32;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

    private final UserDAO userDAO;
    private final PasswordResetDAO passwordResetDAO;

    public PasswordResetService() {
        this(new UserDAO(), new PasswordResetDAO());
    }

    public PasswordResetService(UserDAO userDAO, PasswordResetDAO passwordResetDAO) {
        this.userDAO = userDAO;
        this.passwordResetDAO = passwordResetDAO;
    }

    /**
     * Validate form yêu cầu quên mật khẩu (kiểm tra định dạng email và sự tồn tại trong DB).
     */
    public Map<String, String> validateForgotPassword(ForgotPasswordForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String email = form.getEmail();

        if (email.isEmpty()) {
            errors.put("email", Messages.format(Messages.MSG02, "Email"));
        } else if (email.length() > MAX_EMAIL) {
            errors.put("email", Messages.format(Messages.MSG08, MAX_EMAIL));
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", Messages.REGISTER_EMAIL_INVALID);
        } else {
            User user = userDAO.findByEmail(email);
            if (user == null) {
                errors.put("email", Messages.FORGOT_PASSWORD_EMAIL_NOT_FOUND);
            } else if (!"Active".equalsIgnoreCase(user.getStatus())) {
                errors.put("email", Messages.LOGIN_ACCOUNT_INACTIVE);
            }
        }

        return errors;
    }

    /**
     * Bắt đầu quy trình khôi phục: sinh token an toàn, lưu DB và gửi email chứa link đặt lại mật khẩu.
     *
     * @param form       Form chứa email người dùng
     * @param appBaseUrl URL gốc của ứng dụng (vd: http://localhost:9999/SE2053-SWP391-G2)
     * @return Mã token vừa được sinh
     */
    public String initiatePasswordReset(ForgotPasswordForm form, String appBaseUrl) throws SQLException, ValidationException {
        Map<String, String> errors = validateForgotPassword(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User user = userDAO.findByEmail(form.getEmail());
        if (user == null) {
            throw new ValidationException(Map.of("email", Messages.FORGOT_PASSWORD_EMAIL_NOT_FOUND));
        }

        int expiryMinutes = AppConfig.getInt("mail.passwordReset.expiryMinutes", 15);
        String token = UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());

        // Vô hiệu hóa mọi link/token trước đó của email này
        passwordResetDAO.invalidatePreviousTokens(form.getEmail());

        // Lưu token mới vào DB
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUserId(user.getUserId());
        prt.setEmail(user.getEmail());
        prt.setToken(token);
        prt.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        prt.setUsed(false);
        prt.setCreatedAt(LocalDateTime.now());
        passwordResetDAO.insert(prt);

        // Tạo đường link khôi phục mật khẩu hoàn chỉnh
        String resetLink = appBaseUrl + "/reset-password?token=" + token;

        // Gửi email bất đồng bộ
        EmailService.sendPasswordResetLinkAsync(user.getEmail(), user.getFullName(), resetLink, expiryMinutes);

        return token;
    }

    /**
     * Kiểm tra tính hợp lệ của token khi người dùng truy cập link khôi phục.
     */
    public PasswordResetToken validateToken(String tokenStr) throws SQLException, ValidationException {
        if (tokenStr == null || tokenStr.trim().isEmpty()) {
            throw new ValidationException(Map.of("token", Messages.RESET_PASSWORD_TOKEN_INVALID));
        }
        PasswordResetToken token = passwordResetDAO.findValidToken(tokenStr.trim());
        if (token == null) {
            throw new ValidationException(Map.of("token", Messages.RESET_PASSWORD_TOKEN_INVALID));
        }
        return token;
    }

    /**
     * Validate mật khẩu mới và xác nhận mật khẩu.
     */
    public Map<String, String> validateNewPassword(ResetPasswordForm form, User user) {
        Map<String, String> errors = new LinkedHashMap<>();

        String newPassword = form.getNewPassword();
        if (newPassword.isEmpty()) {
            errors.put("newPassword", Messages.format(Messages.MSG02, "Mật khẩu mới"));
        } else if (newPassword.length() < MIN_PASSWORD || newPassword.length() > MAX_PASSWORD) {
            errors.put("newPassword", Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD));
        } else if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            errors.put("newPassword", Messages.REGISTER_PASSWORD_WEAK);
        } else if (user != null && PasswordUtil.verify(newPassword, user.getPasswordHash())) {
            errors.put("newPassword", Messages.RESET_PASSWORD_SAME_AS_OLD);
        }

        String confirmPassword = form.getConfirmPassword();
        if (confirmPassword.isEmpty()) {
            errors.put("confirmPassword", Messages.format(Messages.MSG02, "Xác nhận mật khẩu"));
        } else if (!newPassword.equals(confirmPassword)) {
            errors.put("confirmPassword", Messages.REGISTER_PASSWORD_MISMATCH);
        }

        return errors;
    }

    /**
     * Hoàn tất đặt lại mật khẩu mới: kiểm tra token, băm mật khẩu bằng PBKDF2 và cập nhật DB.
     * Trả về tài khoản vừa đổi mật khẩu để ghi nhật ký hệ thống (UC-32).
     */
    public User resetPassword(ResetPasswordForm form) throws SQLException, ValidationException {
        PasswordResetToken token = validateToken(form.getToken());

        User user = userDAO.findById(token.getUserId());
        if (user == null || !"Active".equalsIgnoreCase(user.getStatus())) {
            throw new ValidationException(Map.of("general", Messages.LOGIN_ACCOUNT_INACTIVE));
        }

        Map<String, String> errors = validateNewPassword(form, user);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // Băm mật khẩu mới bằng PBKDF2 + salt ngẫu nhiên
        String newHash = PasswordUtil.hash(form.getNewPassword());

        // Cập nhật mật khẩu trong CSDL
        userDAO.updatePassword(user.getUserId(), newHash);

        // Đánh dấu token đã được sử dụng
        passwordResetDAO.markUsed(token.getTokenId());
        return user;
    }
}
