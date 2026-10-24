package vn.edu.fpt.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.Gender;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.PasswordUtil;

// UC-10: Nghiệp vụ xem và cập nhật hồ sơ cá nhân của khách hàng
public class ProfileService {
    private static final LocalDate MIN_DATE_OF_BIRTH = LocalDate.of(1900, 1, 1);
    private static final String MIN_DOB_LABEL = "01/01/1900";
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
        return userDAO.findProfileById(userId);
    }

    /**
     * Cập nhật Họ tên, Số điện thoại, Ngày sinh, Giới tính. Trả về User mới nhất từ DB để controller làm mới session.
     */
    public User updateProfile(int userId, ProfileForm form) throws SQLException, ValidationException {
        User user = requireUser(userId);
        Map<String, String> errors = new LinkedHashMap<>();

        String fullName = form.getFullName();
        String fullNameError = FullNameRule.validate(fullName);
        if (fullNameError != null) {
            errors.put("fullName", fullNameError);
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

        // Ngày sinh và giới tính không bắt buộc vì các tài khoản cũ chưa có dữ liệu này
        LocalDate dateOfBirth = null;
        if (!form.getDateOfBirth().isEmpty()) {
            try {
                dateOfBirth = LocalDate.parse(form.getDateOfBirth());
                if (dateOfBirth.isBefore(MIN_DATE_OF_BIRTH) || dateOfBirth.isAfter(LocalDate.now())) {
                    errors.put("dateOfBirth", Messages.format(Messages.PROFILE_DOB_OUT_OF_RANGE, MIN_DOB_LABEL));
                }
            } catch (DateTimeParseException e) {
                errors.put("dateOfBirth", Messages.PROFILE_DOB_INVALID);
            }
        }

        String gender = form.getGender().isEmpty() ? null : form.getGender();
        if (gender != null && !Gender.isValid(gender)) {
            errors.put("gender", Messages.PROFILE_GENDER_INVALID);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            userDAO.updateProfile(user.getUserId(), fullName, phone, dateOfBirth, gender);
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

    /**
     * AF1 bước 4: phiên đăng nhập chỉ còn hiệu lực khi mật khẩu băm lưu lúc đăng nhập vẫn trùng với DB.
     * Đổi mật khẩu ở một phiên sẽ làm mọi phiên khác của tài khoản đó hết hiệu lực.
     */
    public boolean isSessionCredentialCurrent(User sessionUser) throws SQLException {
        if (sessionUser == null || sessionUser.getPasswordHash() == null) {
            return false;
        }
        User user = userDAO.findById(sessionUser.getUserId());
        return user != null && sessionUser.getPasswordHash().equals(user.getPasswordHash());
    }

    private User requireUser(int userId) throws SQLException, ValidationException {
        User user = userDAO.findProfileById(userId);
        if (user == null) {
            throw new ValidationException(Map.of("general", Messages.PROFILE_NOT_FOUND));
        }
        return user;
    }
}
