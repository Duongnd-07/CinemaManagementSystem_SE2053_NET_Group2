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
import vn.edu.fpt.model.EmailVerification;
import vn.edu.fpt.util.DBContext;

// DAO quản lý bảng EmailVerification (UC-01: Lưu trữ và kiểm tra mã OTP)
public class EmailVerificationDAO {
    private static final Logger LOGGER = Logger.getLogger(EmailVerificationDAO.class.getName());
    private static volatile boolean tableChecked = false;

    public EmailVerificationDAO() {
        ensureTableExists();
    }

    /**
     * Tự động tạo bảng EmailVerification nếu database chưa có (tránh lỗi nếu chưa chạy file .sql).
     */
    private void ensureTableExists() {
        if (tableChecked) {
            return;
        }
        String sql = """
                IF OBJECT_ID('EmailVerification', 'U') IS NULL
                BEGIN
                    CREATE TABLE EmailVerification (
                        VerificationID INT IDENTITY(1,1) PRIMARY KEY,
                        Email VARCHAR(100) NOT NULL,
                        OtpCode VARCHAR(10) NOT NULL,
                        ExpiryTime DATETIME NOT NULL,
                        IsUsed BIT NOT NULL DEFAULT 0,
                        CreatedAt DATETIME NOT NULL DEFAULT GETDATE()
                    );
                END
                """;
        try (Connection conn = DBContext.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            tableChecked = true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không thể kiểm tra/tạo bảng EmailVerification tự động", e);
        }
    }

    /**
     * Vô hiệu hóa các mã OTP trước đó của email này (khi người dùng yêu cầu gửi mã mới).
     */
    public void invalidatePreviousOtps(String email) throws SQLException {
        String sql = "UPDATE EmailVerification SET IsUsed = 1 WHERE LOWER(Email) = LOWER(?) AND IsUsed = 0";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            ps.executeUpdate();
        }
    }

    /**
     * Lưu mã OTP mới vào database.
     */
    public int insert(EmailVerification ev) throws SQLException {
        String sql = """
                INSERT INTO EmailVerification (Email, OtpCode, ExpiryTime, IsUsed, CreatedAt)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ev.getEmail().trim().toLowerCase());
            ps.setString(2, ev.getOtpCode().trim());
            ps.setTimestamp(3, Timestamp.valueOf(ev.getExpiryTime()));
            ps.setBoolean(4, ev.isUsed());
            ps.setTimestamp(5, Timestamp.valueOf(ev.getCreatedAt() == null ? LocalDateTime.now() : ev.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    ev.setVerificationId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    /**
     * Tìm mã OTP hợp lệ mới nhất cho email (chưa dùng và chưa quá hạn).
     */
    public EmailVerification findLatestValidOtp(String email, String otpCode) throws SQLException {
        String sql = """
                SELECT TOP 1 VerificationID, Email, OtpCode, ExpiryTime, IsUsed, CreatedAt
                FROM EmailVerification
                WHERE LOWER(Email) = LOWER(?)
                  AND OtpCode = ?
                  AND IsUsed = 0
                  AND ExpiryTime > GETDATE()
                ORDER BY VerificationID DESC
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            ps.setString(2, otpCode == null ? "" : otpCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EmailVerification ev = new EmailVerification();
                    ev.setVerificationId(rs.getInt("VerificationID"));
                    ev.setEmail(rs.getString("Email"));
                    ev.setOtpCode(rs.getString("OtpCode"));
                    ev.setExpiryTime(rs.getTimestamp("ExpiryTime").toLocalDateTime());
                    ev.setUsed(rs.getBoolean("IsUsed"));
                    ev.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
                    return ev;
                }
            }
        }
        return null;
    }

    /**
     * Đánh dấu mã OTP đã được sử dụng sau khi xác thực thành công.
     */
    public void markUsed(int verificationId) throws SQLException {
        String sql = "UPDATE EmailVerification SET IsUsed = 1 WHERE VerificationID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, verificationId);
            ps.executeUpdate();
        }
    }
}
