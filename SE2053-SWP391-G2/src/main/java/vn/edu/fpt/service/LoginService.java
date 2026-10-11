package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-02: Nghiệp vụ Đăng nhập & Xác thực người dùng theo 4 vai trò (Admin, Manager, Staff, Customer)
public class LoginService {
    private static final Logger LOGGER = Logger.getLogger(LoginService.class.getName());

    private final UserDAO userDAO;

    public LoginService() {
        this(new UserDAO());
    }

    public LoginService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     *
     * @param form Đối tượng LoginForm chứa identifier (email/số điện thoại) và password
     * @return Đối tượng User nếu đăng nhập thành công
     * @throws ValidationException Nếu thiếu thông tin, sai tài khoản/mật khẩu hoặc tài khoản bị khóa
     * @throws SQLException        Nếu xảy ra lỗi truy vấn cơ sở dữ liệu
     */
    public User authenticate(LoginForm form) throws ValidationException, SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        String identifier = form.getIdentifier() == null ? "" : form.getIdentifier().trim();
        String password = form.getPassword() == null ? "" : form.getPassword();

        // 1. Kiểm tra các trường bắt buộc
        if (identifier.isEmpty()) {
            errors.put("identifier", Messages.LOGIN_REQUIRED_IDENTIFIER);
        }
        if (password.isEmpty()) {
            errors.put("password", Messages.LOGIN_REQUIRED_PASSWORD);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 2. Tìm người dùng theo Email hoặc Số điện thoại
        User user = userDAO.findByEmailOrPhone(identifier);
        if (user == null) {
            // Không phân biệt sai tài khoản hay sai mật khẩu để bảo vệ chống dò quét (enumeration)
            errors.put("auth", Messages.LOGIN_FAILED);
            throw new ValidationException(errors);
        }

        // 3. Kiểm tra tính khớp của mật khẩu
        boolean passwordValid = PasswordUtil.verify(password, user.getPasswordHash());
        if (!passwordValid) {
            errors.put("auth", Messages.LOGIN_FAILED);
            throw new ValidationException(errors);
        }

        // 4. Kiểm tra trạng thái tài khoản (chỉ tài khoản "Active" mới được phép đăng nhập)
        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            errors.put("auth", Messages.LOGIN_ACCOUNT_INACTIVE);
            throw new ValidationException(errors);
        }

        // 5. Nâng cấp tự động chuẩn mật khẩu PBKDF2 nếu tài khoản dùng hash cũ hoặc seed dev
        if (PasswordUtil.needsRehash(user.getPasswordHash())) {
            try {
                String upgradedHash = PasswordUtil.hash(password);
                userDAO.updatePassword(user.getUserId(), upgradedHash);
                user.setPasswordHash(upgradedHash);
                LOGGER.info("Tự động nâng cấp thuật toán băm PBKDF2 cho User ID: " + user.getUserId());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Không thể nâng cấp hash mật khẩu cho User ID: " + user.getUserId(), e);
            }
        }

        return user;
    }
}
