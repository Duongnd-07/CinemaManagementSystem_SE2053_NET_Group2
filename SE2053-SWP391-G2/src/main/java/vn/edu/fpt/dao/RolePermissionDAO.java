package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// UC-31: đọc và ghi các quyền Admin đã thay đổi trong bảng RolePermission.
public class RolePermissionDAO {

    // Khóa "vai trò:quyền" -> được phép hay không
    public Map<String, Boolean> findAll(Connection conn) throws SQLException {
        return findAll(conn, false);
    }

    // forUpdate: khóa trong transaction lưu để so sánh và ghi log đúng khi 2 Admin cùng lưu
    public Map<String, Boolean> findAll(Connection conn, boolean forUpdate) throws SQLException {
        String sql = "SELECT RoleName, PermissionKey, IsAllowed FROM RolePermission"
                + (forUpdate ? " WITH (UPDLOCK, HOLDLOCK)" : "");
        Map<String, Boolean> stored = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stored.put(rs.getString("RoleName") + ":" + rs.getString("PermissionKey"), rs.getBoolean("IsAllowed"));
            }
        }
        return stored;
    }

    public void save(Connection conn, String role, String permissionKey, boolean allowed) throws SQLException {
        String update = "UPDATE RolePermission SET IsAllowed = ?, UpdatedAt = GETDATE() WHERE RoleName = ? AND PermissionKey = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setBoolean(1, allowed);
            ps.setString(2, role);
            ps.setString(3, permissionKey);
            if (ps.executeUpdate() > 0) {
                return;
            }
        }
        String insert = "INSERT INTO RolePermission (RoleName, PermissionKey, IsAllowed, UpdatedAt) VALUES (?, ?, ?, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, role);
            ps.setString(2, permissionKey);
            ps.setBoolean(3, allowed);
            ps.executeUpdate();
        }
    }
}
