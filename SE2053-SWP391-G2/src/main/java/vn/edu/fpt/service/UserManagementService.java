package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;
import vn.edu.fpt.util.PasswordUtil;

/**
 * Nghiệp vụ Quản lý tài khoản & Phân quyền dành riêng cho Admin (UC-30).
 * Tuân thủ GB-12: Không hard-delete tài khoản; chỉ khóa hoặc đổi trạng thái.
 */
public class UserManagementService {
    public static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_FULLNAME = 50;
    private static final int MAX_EMAIL = 100;
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 32;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

    private static final List<String> VALID_STATUSES = List.of("Active", "Suspended", "Deactivated");

    private final UserDAO userDAO;

    public UserManagementService() {
        this(new UserDAO());
    }

    public UserManagementService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Lấy danh sách tài khoản theo trang kèm bộ lọc và phân trang dùng Page utility.
     */
    public Page<User> listUsers(String keyword, String role, String status, int page, int pageSize)
            throws SQLException {
        int total = userDAO.countUsers(keyword, role, status);
        return Page.of(page, pageSize, total,
                (offset, limit) -> userDAO.findUsers(keyword, role, status, offset, limit));
    }

    /**
     * Lấy danh sách tài khoản theo trang kèm bộ lọc (offset/limit).
     */
    public List<User> getUsers(String keyword, String role, String status, int page, int pageSize)
            throws SQLException {
        int offset = Math.max(0, (page - 1) * pageSize);
        return userDAO.findUsers(keyword, role, status, offset, pageSize);
    }

    /**
     * Đếm tổng số tài khoản thỏa mãn bộ lọc.
     */
    public int countUsers(String keyword, String role, String status) throws SQLException {
        return userDAO.countUsers(keyword, role, status);
    }

    /**
     * Lấy số liệu thống kê tài khoản theo từng vai trò.
     */
    public Map<String, Integer> getStats() throws SQLException {
        return userDAO.countStatsByRole();
    }

    /**
     * Lấy thông tin chi tiết một tài khoản theo ID.
     */
    public User getUserById(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    /**
     * Cập nhật thông tin cơ bản của tài khoản (Họ và tên, Số điện thoại).
     */
    public void updateUserProfile(int targetUserId, String fullName, String phone)
            throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", Messages.USER_NOT_FOUND));
        }

        String trimmedName = fullName == null ? "" : fullName.trim();
        if (trimmedName.isEmpty()) {
            errors.put("fullName", Messages.format(Messages.MSG02, "Họ và tên"));
        } else if (trimmedName.length() > MAX_FULLNAME) {
            errors.put("fullName", Messages.format(Messages.MSG08, MAX_FULLNAME));
        }

        String trimmedPhone = phone == null ? "" : phone.trim();
        if (trimmedPhone.isEmpty()) {
            errors.put("phone", Messages.format(Messages.MSG02, "Số điện thoại"));
        } else if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            errors.put("phone", Messages.REGISTER_PHONE_INVALID);
        } else if (userDAO.existsByPhoneExceptUser(trimmedPhone, targetUserId)) {
            errors.put("phone", Messages.REGISTER_PHONE_DUPLICATE);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        userDAO.updateProfile(targetUserId, trimmedName, trimmedPhone);
    }

    /**
     * Phân quyền (đổi Role) cho tài khoản.
     * Ràng buộc an toàn:
     * 1. Admin không được tự đổi quyền của chính mình.
     * 2. Không được hạ quyền tài khoản Admin hoạt động duy nhất trong hệ thống.
     */
    public void updateUserRole(int currentAdminId, int targetUserId, String newRole)
            throws SQLException, ValidationException {
        if (!Role.isValid(newRole)) {
            throw new ValidationException(Map.of("role", Messages.USER_ROLE_INVALID));
        }
        if (Role.ADMIN.equalsIgnoreCase(newRole)) {
            throw new ValidationException(Map.of("role", Messages.USER_ROLE_ADMIN_NOT_ASSIGNABLE));
        }
        if (currentAdminId == targetUserId) {
            throw new ValidationException(Map.of("role", Messages.USER_ROLE_SELF_LOCKED));
        }
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", Messages.USER_NOT_FOUND));
        }

        // Quy tắc bảo vệ Admin cuối cùng
        if (Role.ADMIN.equalsIgnoreCase(targetUser.getRole()) && !Role.ADMIN.equalsIgnoreCase(newRole)) {
            if (userDAO.countActiveAdmins() <= 1) {
                throw new ValidationException(Map.of("role", Messages.USER_LAST_ADMIN_ROLE_LOCKED));
            }
        }

        userDAO.updateRole(targetUserId, newRole);
    }

    /**
     * Cập nhật trạng thái tài khoản (Active, Suspended, Deactivated).
     * Ràng buộc an toàn:
     * 1. Admin không được tự khóa hoặc hủy kích hoạt tài khoản của chính mình.
     * 2. Không được khóa tài khoản Admin hoạt động duy nhất trong hệ thống.
     * 3. Tuân thủ GB-12: Không xóa cứng khỏi CSDL.
     */
    public void updateUserStatus(int currentAdminId, int targetUserId, String newStatus)
            throws SQLException, ValidationException {
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            throw new ValidationException(Map.of("status", Messages.USER_STATUS_INVALID));
        }
        if (currentAdminId == targetUserId && !"Active".equalsIgnoreCase(newStatus)) {
            throw new ValidationException(Map.of("status", Messages.USER_STATUS_SELF_LOCKED));
        }
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", Messages.USER_NOT_FOUND));
        }

        // Quy tắc bảo vệ Admin cuối cùng
        if (Role.ADMIN.equalsIgnoreCase(targetUser.getRole()) && !"Active".equalsIgnoreCase(newStatus)) {
            if (userDAO.countActiveAdmins() <= 1) {
                throw new ValidationException(Map.of("status", Messages.USER_LAST_ADMIN_STATUS_LOCKED));
            }
        }

        userDAO.updateStatus(targetUserId, newStatus);
    }

    /**
     * Tạo tài khoản mới trực tiếp từ giao diện Admin (cho nhân viên, quản lý hoặc quản trị viên).
     */
    public User createUser(String fullName, String email, String phone, String role, String password)
            throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Họ và tên
        String trimmedName = fullName == null ? "" : fullName.trim();
        if (trimmedName.isEmpty()) {
            errors.put("fullName", Messages.format(Messages.MSG02, "Họ và tên"));
        } else if (trimmedName.length() > MAX_FULLNAME) {
            errors.put("fullName", Messages.format(Messages.MSG08, MAX_FULLNAME));
        }

        // 2. Email
        String trimmedEmail = email == null ? "" : email.trim().toLowerCase();
        if (trimmedEmail.isEmpty()) {
            errors.put("email", Messages.format(Messages.MSG02, "Email"));
        } else if (trimmedEmail.length() > MAX_EMAIL) {
            errors.put("email", Messages.format(Messages.MSG08, MAX_EMAIL));
        } else if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            errors.put("email", Messages.REGISTER_EMAIL_INVALID);
        } else if (userDAO.existsByEmail(trimmedEmail)) {
            errors.put("email", Messages.REGISTER_EMAIL_DUPLICATE);
        }

        // 3. Số điện thoại
        String trimmedPhone = phone == null ? "" : phone.trim();
        if (trimmedPhone.isEmpty()) {
            errors.put("phone", Messages.format(Messages.MSG02, "Số điện thoại"));
        } else if (!PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            errors.put("phone", Messages.REGISTER_PHONE_INVALID);
        } else if (userDAO.existsByPhone(trimmedPhone)) {
            errors.put("phone", Messages.REGISTER_PHONE_DUPLICATE);
        }

        // 4. Vai trò
        if (role == null || !Role.isValid(role)) {
            errors.put("role", Messages.USER_ROLE_INVALID);
        }

        // 5. Mật khẩu
        String trimmedPass = password == null ? "" : password;
        if (trimmedPass.isEmpty()) {
            errors.put("password", Messages.format(Messages.MSG02, "Mật khẩu"));
        } else if (trimmedPass.length() < MIN_PASSWORD || trimmedPass.length() > MAX_PASSWORD) {
            errors.put("password", Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD));
        } else if (!PASSWORD_PATTERN.matcher(trimmedPass).matches()) {
            errors.put("password", Messages.REGISTER_PASSWORD_WEAK);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String passwordHash = PasswordUtil.hash(trimmedPass);
        User user = new User();
        user.setFullName(trimmedName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setPointBalance(0);
        user.setStatus("Active");

        int id = userDAO.insert(user);
        user.setUserId(id);
        return user;
    }

    /**
     * Admin đặt lại mật khẩu cho người dùng.
     */
    public void resetPassword(int targetUserId, String newPassword)
            throws SQLException, ValidationException {
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", Messages.USER_NOT_FOUND));
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD || newPassword.length() > MAX_PASSWORD) {
            throw new ValidationException(Map.of("newPassword",
                    Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD)));
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ValidationException(Map.of("newPassword", Messages.REGISTER_PASSWORD_WEAK));
        }

        String hash = PasswordUtil.hash(newPassword);
        userDAO.updatePassword(targetUserId, hash);
    }
}
