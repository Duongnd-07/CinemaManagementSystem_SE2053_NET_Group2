package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import vn.edu.fpt.util.DBContext;

// Đọc tham số hệ thống (UC-33) từ bảng SystemConfig; chưa cấu hình hoặc sai định dạng thì dùng giá trị mặc định.
// Bản nhận Connection dùng lại kết nối của request (mở kết nối mới tốn vài trăm ms).
public class SystemConfigDAO {

    public int getInt(String key, int defaultValue) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getInt(conn, key, defaultValue);
        }
    }

    public int getInt(Connection conn, String key, int defaultValue) throws SQLException {
        String value = find(conn, key);
        try {
            return value == null ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public BigDecimal getDecimal(Connection conn, String key, BigDecimal defaultValue) throws SQLException {
        String value = find(conn, key);
        try {
            return value == null ? defaultValue : new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String find(Connection conn, String key) throws SQLException {
        String sql = "SELECT ConfigValue FROM SystemConfig WHERE ConfigKey = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
