package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-10: Nghiệp vụ xem và cập nhật hồ sơ cá nhân của khách hàng
public class ProfileService {
    // Giữ cùng giới hạn với UC-01 để dữ liệu đăng ký và hồ sơ nhất quán
    private static final int MAX_FULLNAME = 50;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 32;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    // BR-PROF-03: ít nhất 1 chữ thường, 1 chữ hoa, 1 chữ số
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

    private final UserDAO userDAO;

    public ProfileService() {
        this(new UserDAO());
    }

    public ProfileService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User findById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    /**
     * Cập nhật Họ tên và Số điện thoại. Trả về User mới nhất từ DB để controller làm mới session.
     */
    public User updateProfile(int userId, ProfileForm form) throws SQLException, ValidationException {
        User user = requireUser(userId);
        Map<String, String> errors = new LinkedHashMap<>();

        String fullName = form.getFullName();
        if (fullName.isEmpty()) {
            errors.put("fullName", Messages.format(Messages.MSG02, "Họ và tên"));
        } else if (fullName.length() > MAX_FULLNAME) {
            errors.put("fullName", Messages.format(Messages.MSG08, MAX_FULLNAME));
        }

        String phone = form.getPhone();
        if (phone.isEmpty()) {
            errors.put("phone", Messages.format(Messages.MSG02, "Số điện thoại"));
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put("phone", Messages.REGISTER_PHONE_INVALID);
        } else if (userDAO.existsByPhoneExceptUser(phone, user.getUserId())) {
            // BR-PROF-02: SĐT là khóa tra cứu thành viên tại quầy (UC-19) nên phải duy nhất
            errors.put("phone", Messages.REGISTER_PHONE_DUPLICATE);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            userDAO.updateProfile(user.getUserId(), fullName, phone);
        } catch (SQLException e) {
            // Hai người cùng đổi sang một SĐT ở cùng thời điểm: ràng buộc UNIQUE của DB chặn lại
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of("phone", Messages.REGISTER_PHONE_DUPLICATE));
            }
            throw e;
        }
        return requireUser(userId);
    }

    /**
     * AF1: Đổi mật khẩu sau khi xác thực mật khẩu hiện tại (BR-PROF-04).
     */
    public User changePassword(int userId, ChangePasswordForm form) throws SQLException, ValidationException {
        User user = requireUser(userId);
        Map<String, String> errors = new LinkedHashMap<>();

        String current = form.getCurrentPassword();
        if (current.isEmpty()) {
            errors.put("currentPassword", Messages.format(Messages.MSG02, "Mật khẩu hiện tại"));
        } else if (!PasswordUtil.verify(current, user.getPasswordHash())) {
            errors.put("currentPassword", Messages.PROFILE_CURRENT_PASSWORD_WRONG);
        }

        String newPassword = form.getNewPassword();
        if (newPassword.isEmpty()) {
            errors.put("newPassword", Messages.format(Messages.MSG02, "Mật khẩu mới"));
        } else if (newPassword.length() < MIN_PASSWORD || newPassword.length() > MAX_PASSWORD) {
            errors.put("newPassword", Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD));
        } else if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            errors.put("newPassword", Messages.REGISTER_PASSWORD_WEAK);
        } else if (newPassword.equals(current)) {
            errors.put("newPassword", Messages.PROFILE_PASSWORD_SAME_AS_OLD);
        }

        String confirm = form.getConfirmPassword();
        if (confirm.isEmpty()) {
            errors.put("confirmPassword", Messages.format(Messages.MSG02, "Xác nhận mật khẩu mới"));
        } else if (!confirm.equals(newPassword)) {
            errors.put("confirmPassword", Messages.REGISTER_PASSWORD_MISMATCH);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        userDAO.updatePassword(user.getUserId(), PasswordUtil.hash(newPassword));
        return requireUser(userId);
    }

    private User requireUser(int userId) throws SQLException, ValidationException {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new ValidationException(Map.of("general", Messages.PROFILE_NOT_FOUND));
        }
        return user;
    }
}
