package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.DBContext;

// DAO quản lý bảng [User] - Truy vấn SQL thuần qua PreparedStatement
public class UserDAO {

    /**
     * Kiểm tra Email đã được đăng ký trong hệ thống chưa (không phân biệt hoa thường).
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM [User] WHERE LOWER(Email) = LOWER(?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Kiểm tra Số điện thoại đã được đăng ký trong hệ thống chưa.
     */
    public boolean existsByPhone(String phone) throws SQLException {
        String sql = "SELECT 1 FROM [User] WHERE Phone = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone == null ? "" : phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Thêm tài khoản thành viên mới (Customer) sau khi xác thực email thành công.
     *
     * @param user Đối tượng User chứa FullName, Email, Phone, PasswordHash
     * @return UserID vừa được tạo
     */
    public int insert(User user) throws SQLException {
        String sql = """
                INSERT INTO [User] (FullName, Email, Phone, PasswordHash, Role, PointBalance, Status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPhone().trim());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole() == null ? Role.CUSTOMER : user.getRole());
            ps.setInt(6, user.getPointBalance());
            ps.setString(7, user.getStatus() == null ? "Active" : user.getStatus());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    user.setUserId(generatedId);
                    return generatedId;
                }
            }
        }
        return 0;
    }

    /**
     * Tìm thông tin User theo Email.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = """
                SELECT UserID, FullName, Email, Phone, PasswordHash, Role, PointBalance, Status
                FROM [User] WHERE LOWER(Email) = LOWER(?)
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm thông tin User theo ID.
     */
    public User findById(int userId) throws SQLException {
        String sql = """
                SELECT UserID, FullName, Email, Phone, PasswordHash, Role, PointBalance, Status
                FROM [User] WHERE UserID = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * UC-02: Tìm thông tin User theo Email hoặc Số điện thoại để xử lý đăng nhập.
     *
     * @param identifier Email hoặc Số điện thoại người dùng nhập
     * @return Đối tượng User tương ứng, hoặc null nếu không tồn tại
     */
    public User findByEmailOrPhone(String identifier) throws SQLException {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = identifier.trim();
        String sql = """
                SELECT UserID, FullName, Email, Phone, PasswordHash, Role, PointBalance, Status
                FROM [User]
                WHERE LOWER(Email) = LOWER(?) OR Phone = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Cập nhật mật khẩu băm mới (dùng khi nâng cấp thuật toán băm hoặc đổi mật khẩu).
     */
    public boolean updatePassword(int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE [User] SET PasswordHash = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * UC-10: Kiểm tra Số điện thoại đã thuộc về một tài khoản khác (bỏ qua chính tài khoản đang sửa).
     */
    public boolean existsByPhoneExceptUser(String phone, int userId) throws SQLException {
        String sql = "SELECT 1 FROM [User] WHERE Phone = ? AND UserID <> ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone == null ? "" : phone.trim());
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * UC-10: Cập nhật Họ tên và Số điện thoại. Email không nằm trong câu lệnh vì là định danh bất biến (BR-PROF-01).
     */
    public boolean updateProfile(int userId, String fullName, String phone) throws SQLException {
        String sql = "UPDATE [User] SET FullName = ?, Phone = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("UserID"));
        user.setFullName(rs.getString("FullName"));
        user.setEmail(rs.getString("Email"));
        user.setPhone(rs.getString("Phone"));
        user.setPasswordHash(rs.getString("PasswordHash"));
        user.setRole(rs.getString("Role"));
        user.setPointBalance(rs.getInt("PointBalance"));
        user.setStatus(rs.getString("Status"));
        return user;
    }
}
