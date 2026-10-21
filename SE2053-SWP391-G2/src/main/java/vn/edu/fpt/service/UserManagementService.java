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
import vn.edu.fpt.util.PasswordUtil;

// Nghiệp vụ Quản lý tài khoản & Phân quyền dành riêng cho Admin
public class UserManagementService {
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
     * Lấy danh sách tài khoản theo trang kèm bộ lọc.
     */
    public List<User> getUsers(String keyword, String role, String status, int page, int pageSize) throws SQLException {
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
     * Phân quyền (đổi Role) cho tài khoản.
     * Ràng buộc an toàn: Admin không được tự đổi quyền của chính mình.
     */
    public void updateUserRole(int currentAdminId, int targetUserId, String newRole) throws SQLException, ValidationException {
        if (!Role.isValid(newRole)) {
            throw new ValidationException(Map.of("role", "Vai trò không hợp lệ. Chỉ chấp nhận Admin, Manager, Staff hoặc Customer."));
        }
        if (currentAdminId == targetUserId) {
            throw new ValidationException(Map.of("role", "Bạn không thể tự thay đổi vai trò quản trị của chính tài khoản đang đăng nhập."));
        }
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", "Không tìm thấy tài khoản người dùng cần cập nhật."));
        }

        userDAO.updateRole(targetUserId, newRole);
    }

    /**
     * Cập nhật trạng thái tài khoản (Active, Suspended, Deactivated).
     * Ràng buộc an toàn: Admin không được tự khóa hoặc hủy kích hoạt tài khoản của chính mình.
     */
    public void updateUserStatus(int currentAdminId, int targetUserId, String newStatus) throws SQLException, ValidationException {
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            throw new ValidationException(Map.of("status", "Trạng thái không hợp lệ. Chỉ chấp nhận Active, Suspended hoặc Deactivated."));
        }
        if (currentAdminId == targetUserId && !"Active".equalsIgnoreCase(newStatus)) {
            throw new ValidationException(Map.of("status", "Bạn không thể tự khóa tài khoản quản trị của chính mình."));
        }
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", "Không tìm thấy tài khoản người dùng cần cập nhật."));
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
            errors.put("role", "Vai trò không hợp lệ.");
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
    public void resetPassword(int targetUserId, String newPassword) throws SQLException, ValidationException {
        User targetUser = userDAO.findById(targetUserId);
        if (targetUser == null) {
            throw new ValidationException(Map.of("general", "Không tìm thấy người dùng."));
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD || newPassword.length() > MAX_PASSWORD) {
            throw new ValidationException(Map.of("newPassword", Messages.format(Messages.REGISTER_PASSWORD_LENGTH, MIN_PASSWORD, MAX_PASSWORD)));
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ValidationException(Map.of("newPassword", Messages.REGISTER_PASSWORD_WEAK));
        }

        String hash = PasswordUtil.hash(newPassword);
        userDAO.updatePassword(targetUserId, hash);
    }
}
