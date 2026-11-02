package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.fpt.model.Announcement;
import vn.edu.fpt.util.DBContext;

public class AnnouncementDAO {
    public int insert(Connection conn, Announcement announcement) throws SQLException {
        String sql = "INSERT INTO Announcement (SenderID, Title, Message, NotificationType, TargetType, TargetUserID) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, announcement.getSenderId());
            ps.setString(2, announcement.getTitle());
            ps.setString(3, announcement.getMessage());
            ps.setString(4, announcement.getNotificationType());
            ps.setString(5, announcement.getTargetType());
            setNullableInt(ps, 6, announcement.getTargetUserId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Announcement insert returned no generated key");
            }
        }
    }

    public int fanOut(Connection conn, int announcementId, Announcement announcement,
            List<String> roles, Integer targetUserId, int senderId) throws SQLException {
        String sql = "INSERT INTO CustomerNotification (UserID, Title, Message, NotificationType, AnnouncementID) "
                + "SELECT UserID, ?, ?, ?, ? FROM [User]" + recipientWhere(roles, targetUserId);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, announcement.getTitle());
            ps.setString(2, announcement.getMessage());
            ps.setString(3, announcement.getNotificationType());
            ps.setInt(4, announcementId);
            bindRecipients(ps, 5, roles, targetUserId, senderId);
            return ps.executeUpdate();
        }
    }

    public void updateRecipientCount(Connection conn, int id, int count) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Announcement SET RecipientCount = ? WHERE AnnouncementID = ?")) {
            ps.setInt(1, count);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public int countRecipients(List<String> roles, Integer targetUserId, int senderId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [User]" + recipientWhere(roles, targetUserId);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindRecipients(ps, 1, roles, targetUserId, senderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Only placeholder syntax is generated; every role and ID is bound as a parameter.
    private String recipientWhere(List<String> roles, Integer targetUserId) {
        if (roles.isEmpty()) throw new IllegalArgumentException("Recipient roles must not be empty");
        return " WHERE Status = 'Active' AND UserID <> ? AND Role IN ("
                + String.join(",", Collections.nCopies(roles.size(), "?")) + ")"
                + (targetUserId == null ? "" : " AND UserID = ?");
    }

    private void bindRecipients(PreparedStatement ps, int start, List<String> roles,
            Integer targetUserId, int senderId) throws SQLException {
        int index = start;
        ps.setInt(index++, senderId);
        for (String role : roles) ps.setString(index++, role);
        if (targetUserId != null) ps.setInt(index, targetUserId);
    }

    public boolean existsRecentDuplicate(Connection conn, Announcement announcement) throws SQLException {
        String sql = "SELECT TOP (1) 1 FROM Announcement WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE SenderID = ? AND Title = ? AND Message = ? AND TargetType = ? "
                + "AND (TargetUserID = ? OR (TargetUserID IS NULL AND ? IS NULL)) "
                + "AND Status = 'Sent' AND CreatedAt >= DATEADD(SECOND, -60, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, announcement.getSenderId());
            ps.setString(2, announcement.getTitle());
            ps.setString(3, announcement.getMessage());
            ps.setString(4, announcement.getTargetType());
            setNullableInt(ps, 5, announcement.getTargetUserId());
            setNullableInt(ps, 6, announcement.getTargetUserId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int count(String target, String type, String keyword) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Announcement a WHERE 1=1" + historyWhere(target, type, keyword);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindHistory(ps, target, type, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Announcement> search(String target, String type, String keyword, int offset, int limit)
            throws SQLException {
        String sql = "SELECT a.*, sender.FullName AS SenderName, customer.FullName AS TargetUserName, "
                + "customer.Phone AS TargetUserPhone, (SELECT COUNT(*) FROM CustomerNotification n "
                + "WHERE n.AnnouncementID = a.AnnouncementID AND n.ReadAt IS NOT NULL) AS ReadCount "
                + "FROM Announcement a JOIN [User] sender ON sender.UserID = a.SenderID "
                + "LEFT JOIN [User] customer ON customer.UserID = a.TargetUserID WHERE 1=1"
                + historyWhere(target, type, keyword)
                + " ORDER BY a.CreatedAt DESC, a.AnnouncementID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Announcement> announcements = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = bindHistory(ps, target, type, keyword);
            ps.setInt(index++, offset);
            ps.setInt(index, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Announcement announcement = mapAnnouncement(rs);
                    announcement.setSenderName(rs.getString("SenderName"));
                    announcement.setTargetUserName(rs.getString("TargetUserName"));
                    announcement.setTargetUserPhone(rs.getString("TargetUserPhone"));
                    announcement.setReadCount(rs.getInt("ReadCount"));
                    announcements.add(announcement);
                }
            }
        }
        return announcements;
    }

    private String historyWhere(String target, String type, String keyword) {
        return (hasText(target) ? " AND a.TargetType = ?" : "")
                + (hasText(type) ? " AND a.NotificationType = ?" : "")
                + (hasText(keyword) ? " AND a.Title LIKE ?" : "");
    }

    private int bindHistory(PreparedStatement ps, String target, String type, String keyword) throws SQLException {
        int index = 1;
        if (hasText(target)) ps.setString(index++, target);
        if (hasText(type)) ps.setString(index++, type);
        if (hasText(keyword)) ps.setString(index++, "%" + keyword + "%");
        return index;
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    public boolean markRecalled(Connection conn, int id, int managerId) throws SQLException {
        String sql = "UPDATE Announcement SET Status = 'Recalled', RecalledAt = GETDATE(), RecalledBy = ? "
                + "WHERE AnnouncementID = ? AND Status = 'Sent'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public int deleteRecipients(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CustomerNotification WHERE AnnouncementID = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // Read within the successful recall transaction to provide trustworthy audit metadata.
    public Announcement findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM Announcement WHERE AnnouncementID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAnnouncement(rs) : null;
            }
        }
    }

    private Announcement mapAnnouncement(ResultSet rs) throws SQLException {
        Announcement announcement = new Announcement();
        announcement.setAnnouncementId(rs.getInt("AnnouncementID"));
        announcement.setSenderId(rs.getInt("SenderID"));
        announcement.setTitle(rs.getString("Title"));
        announcement.setMessage(rs.getString("Message"));
        announcement.setNotificationType(rs.getString("NotificationType"));
        announcement.setTargetType(rs.getString("TargetType"));
        announcement.setTargetUserId(rs.getObject("TargetUserID", Integer.class));
        announcement.setRecipientCount(rs.getInt("RecipientCount"));
        announcement.setStatus(rs.getString("Status"));
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        announcement.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp recalledAt = rs.getTimestamp("RecalledAt");
        announcement.setRecalledAt(recalledAt == null ? null : recalledAt.toLocalDateTime());
        announcement.setRecalledBy(rs.getObject("RecalledBy", Integer.class));
        return announcement;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }
}
