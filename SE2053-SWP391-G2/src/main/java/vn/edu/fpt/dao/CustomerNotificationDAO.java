package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.CustomerNotification;
import vn.edu.fpt.util.DBContext;

public class CustomerNotificationDAO {
    public int countForUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM CustomerNotification WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<CustomerNotification> findForUser(int userId, int offset, int limit) throws SQLException {
        String sql = "SELECT NotificationID, Title, Message, NotificationType, CreatedAt, ReadAt "
                + "FROM CustomerNotification WHERE UserID = ? ORDER BY CreatedAt DESC, NotificationID DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<CustomerNotification> notifications = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CustomerNotification notification = new CustomerNotification();
                    notification.setNotificationId(rs.getInt("NotificationID"));
                    notification.setTitle(rs.getString("Title"));
                    notification.setMessage(rs.getString("Message"));
                    notification.setNotificationType(rs.getString("NotificationType"));
                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    notification.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                    Timestamp readAt = rs.getTimestamp("ReadAt");
                    notification.setReadAt(readAt == null ? null : readAt.toLocalDateTime());
                    notifications.add(notification);
                }
            }
        }
        return notifications;
    }

    public int markAllRead(int userId) throws SQLException {
        String sql = "UPDATE CustomerNotification SET ReadAt = GETDATE() WHERE UserID = ? AND ReadAt IS NULL";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    public void insert(Connection conn, int userId, String title, String message, String type)
            throws SQLException {
        String sql = "INSERT INTO CustomerNotification (UserID, Title, Message, NotificationType) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }
}
