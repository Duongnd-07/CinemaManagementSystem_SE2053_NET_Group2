package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import vn.edu.fpt.model.SystemConfig;

// UC-33: đọc toàn bộ và ghi tham số hệ thống cho trang quản trị.
// Việc đọc một tham số lẻ trong các UC khác vẫn dùng SystemConfigDAO.
public class SystemConfigManagementDAO {

    public Map<String, SystemConfig> findAll(Connection conn) throws SQLException {
        return findAll(conn, false);
    }

    // forUpdate: khóa các dòng trong transaction lưu để giá trị cũ ghi log đúng khi 2 Admin cùng lưu
    public Map<String, SystemConfig> findAll(Connection conn, boolean forUpdate) throws SQLException {
        String sql = "SELECT ConfigKey, ConfigValue, Description, UpdatedAt FROM SystemConfig"
                + (forUpdate ? " WITH (UPDLOCK, HOLDLOCK)" : "");
        Map<String, SystemConfig> configs = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(rs.getString("ConfigKey"));
                config.setConfigValue(rs.getString("ConfigValue"));
                config.setDescription(rs.getString("Description"));
                Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                config.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
                configs.put(config.getConfigKey(), config);
            }
        }
        return configs;
    }

    public void update(Connection conn, String key, String value) throws SQLException {
        String sql = "UPDATE SystemConfig SET ConfigValue = ?, UpdatedAt = GETDATE() WHERE ConfigKey = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            ps.executeUpdate();
        }
    }

    // Tham số code đã đọc nhưng DB chưa có dòng (chưa chạy migration) thì tạo khi Admin lưu lần đầu
    public void insert(Connection conn, String key, String value, String description) throws SQLException {
        String sql = "INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description, UpdatedAt) VALUES (?, ?, ?, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }
}
