package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.PasswordResetToken;
import vn.edu.fpt.util.DBContext;

// DAO quản lý bảng PasswordResetToken (UC-03: Khôi phục mật khẩu qua link email)
public class PasswordResetDAO {
    private static final Logger LOGGER = Logger.getLogger(PasswordResetDAO.class.getName());
    private static volatile boolean tableChecked = false;

    public PasswordResetDAO() {
        ensureTableExists();
    }

    /**
     * Tự động tạo bảng PasswordResetToken nếu database chưa có (phòng trường hợp chưa chạy migration script).
     */
    private void ensureTableExists() {
        if (tableChecked) {
            return;
        }
        String sql = """
                IF OBJECT_ID('PasswordResetToken', 'U') IS NULL
                BEGIN
                    CREATE TABLE PasswordResetToken (
                        TokenID INT IDENTITY(1,1) PRIMARY KEY,
                        UserID INT NOT NULL CONSTRAINT FK_PasswordResetToken_User REFERENCES [User](UserID),
                        Email VARCHAR(100) NOT NULL,
                        Token VARCHAR(100) NOT NULL,
                        ExpiryTime DATETIME NOT NULL,
                        IsUsed BIT NOT NULL DEFAULT 0,
                        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
                    );
                END;
                IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_PasswordResetToken_Token')
                BEGIN
                    CREATE INDEX IX_PasswordResetToken_Token ON PasswordResetToken (Token, IsUsed, ExpiryTime);
                END;
                """;
        try (Connection conn = DBContext.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            tableChecked = true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không thể kiểm tra/tạo bảng PasswordResetToken tự động", e);
        }
    }

    /**
     * Vô hiệu hóa các link/token khôi phục mật khẩu trước đó của email này.
     */
    public void invalidatePreviousTokens(String email) throws SQLException {
        String sql = "UPDATE PasswordResetToken SET IsUsed = 1 WHERE LOWER(Email) = LOWER(?) AND IsUsed = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            ps.executeUpdate();
        }
    }

    /**
     * Lưu token khôi phục mật khẩu mới vào database.
     */
    public int insert(PasswordResetToken token) throws SQLException {
        String sql = """
                INSERT INTO PasswordResetToken (UserID, Email, Token, ExpiryTime, IsUsed, CreatedAt)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, token.getUserId());
            ps.setString(2, token.getEmail().trim().toLowerCase());
            ps.setString(3, token.getToken().trim());
            ps.setTimestamp(4, Timestamp.valueOf(token.getExpiryTime()));
            ps.setBoolean(5, token.isUsed());
            ps.setTimestamp(6, Timestamp.valueOf(token.getCreatedAt() == null ? LocalDateTime.now() : token.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    token.setTokenId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Tìm token khôi phục mật khẩu hợp lệ (chưa dùng và còn hạn sử dụng).
     */
    public PasswordResetToken findValidToken(String tokenStr) throws SQLException {
        String sql = """
                SELECT TOP 1 TokenID, UserID, Email, Token, ExpiryTime, IsUsed, CreatedAt
                FROM PasswordResetToken
                WHERE Token = ?
                  AND IsUsed = 0
                  AND ExpiryTime > GETDATE()
                ORDER BY TokenID DESC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenStr == null ? "" : tokenStr.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordResetToken prt = new PasswordResetToken();
                    prt.setTokenId(rs.getInt("TokenID"));
                    prt.setUserId(rs.getInt("UserID"));
                    prt.setEmail(rs.getString("Email"));
                    prt.setToken(rs.getString("Token"));
                    prt.setExpiryTime(rs.getTimestamp("ExpiryTime").toLocalDateTime());
                    prt.setUsed(rs.getBoolean("IsUsed"));
                    prt.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
                    return prt;
                }
            }
        }
        return null;
    }

    /**
     * Đánh dấu token đã được sử dụng sau khi đổi mật khẩu thành công.
     */
    public void markUsed(int tokenId) throws SQLException {
        String sql = "UPDATE PasswordResetToken SET IsUsed = 1 WHERE TokenID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        }
    }
}
