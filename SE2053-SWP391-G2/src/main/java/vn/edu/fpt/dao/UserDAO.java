package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Admin: Lấy danh sách tài khoản kèm tìm kiếm, lọc theo vai trò và trạng thái, có phân trang.
     */
    public List<User> findUsers(String keyword, String role, String status, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT UserID, FullName, Email, Phone, PasswordHash, Role, PointBalance, Status
                FROM [User]
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(FullName) LIKE LOWER(?) OR LOWER(Email) LIKE LOWER(?) OR Phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
            sql.append(" AND Role = ?");
            params.add(role.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND Status = ?");
            params.add(status.trim());
        }
        sql.append(" ORDER BY UserID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<User> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        }
        return list;
    }

    /**
     * Admin: Đếm tổng số lượng tài khoản theo điều kiện lọc.
     */
    public int countUsers(String keyword, String role, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM [User] WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(FullName) LIKE LOWER(?) OR LOWER(Email) LIKE LOWER(?) OR Phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
            sql.append(" AND Role = ?");
            params.add(role.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND Status = ?");
            params.add(status.trim());
        }
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Admin: Cập nhật vai trò (Role) của tài khoản.
     */
    public boolean updateRole(int userId, String newRole) throws SQLException {
        String sql = "UPDATE [User] SET Role = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Admin: Cập nhật trạng thái tài khoản (Active, Suspended, Deactivated).
     */
    public boolean updateStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE [User] SET Status = ? WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Admin: Đếm số lượng tài khoản theo từng vai trò để hiển thị thống kê.
     */
    public Map<String, Integer> countStatsByRole() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("Total", 0);
        stats.put(Role.ADMIN, 0);
        stats.put(Role.MANAGER, 0);
        stats.put(Role.STAFF, 0);
        stats.put(Role.CUSTOMER, 0);

        String sql = "SELECT Role, COUNT(*) AS Cnt FROM [User] GROUP BY Role";
        int total = 0;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String r = rs.getString("Role");
                int c = rs.getInt("Cnt");
                stats.put(r, c);
                total += c;
            }
        }
        stats.put("Total", total);
        return stats;
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
