package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.ActivityLog;
import vn.edu.fpt.util.DBContext;

// UC-32: ghi và tra cứu bảng ActivityLog. Không có update/delete (BR-05).
public class ActivityLogDAO {

    private static final String SELECT_COLUMNS = "SELECT LogID, CreatedAt, LogLevel, EventType, Action, UserID, "
            + "ActorName, ActorEmail, ActorRole, TargetType, TargetID, Description, IpAddress FROM ActivityLog ";
    // CI: không phân biệt hoa thường, AI: không phân biệt dấu
    private static final String SEARCH_COLLATION = "Latin1_General_CI_AI";

    public void insert(ActivityLog log) throws SQLException {
        String sql = "INSERT INTO ActivityLog (LogLevel, EventType, Action, UserID, ActorName, ActorEmail, ActorRole, "
                + "TargetType, TargetID, Description, IpAddress) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, log.getLevel());
            ps.setString(idx++, log.getEventType());
            ps.setString(idx++, log.getAction());
            if (log.getUserId() == null) {
                ps.setNull(idx++, Types.INTEGER);
            } else {
                ps.setInt(idx++, log.getUserId());
            }
            ps.setString(idx++, log.getActorName());
            ps.setString(idx++, log.getActorEmail());
            ps.setString(idx++, log.getActorRole());
            ps.setString(idx++, log.getTargetType());
            ps.setString(idx++, log.getTargetId());
            ps.setString(idx++, log.getDescription());
            ps.setString(idx, log.getIpAddress());
            ps.executeUpdate();
        }
    }

    // Đã có dòng log cùng hành động trên cùng đối tượng chưa (dùng cho sự kiện có thể gửi lặp, vd. IPN VNPay)
    public boolean exists(String eventType, String action, String targetType, String targetId) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM ActivityLog WHERE EventType = ? AND Action = ? AND TargetType = ? AND TargetID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventType);
            ps.setString(2, action);
            ps.setString(3, targetType);
            ps.setString(4, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Các tham số null/rỗng được bỏ qua, các điều kiện kết hợp AND (BR-07). toDate tính trọn ngày.
    public List<ActivityLog> search(LocalDate fromDate, LocalDate toDate, String level, String eventType,
            String user, String keyword, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = SELECT_COLUMNS + buildWhere(params, fromDate, toDate, level, eventType, user, keyword)
                + " ORDER BY CreatedAt DESC, LogID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        params.add(offset);
        params.add(limit);
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    public int count(LocalDate fromDate, LocalDate toDate, String level, String eventType, String user,
            String keyword) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM ActivityLog "
                + buildWhere(params, fromDate, toDate, level, eventType, user, keyword);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String buildWhere(List<Object> params, LocalDate fromDate, LocalDate toDate, String level,
            String eventType, String user, String keyword) {
        List<String> conditions = new ArrayList<>();
        if (fromDate != null) {
            conditions.add("CreatedAt >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            conditions.add("CreatedAt < ?");
            params.add(Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        }
        if (level != null && !level.isEmpty()) {
            conditions.add("LogLevel = ?");
            params.add(level);
        }
        if (eventType != null && !eventType.isEmpty()) {
            conditions.add("EventType = ?");
            params.add(eventType);
        }
        addContainsAny(conditions, params, user, "ActorName", "ActorEmail");
        addContainsAny(conditions, params, keyword, "Action", "Description");
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    // Khớp một phần ở ít nhất một trong các cột
    private void addContainsAny(List<String> conditions, List<Object> params, String value, String... columns) {
        if (value == null || value.isEmpty()) {
            return;
        }
        List<String> parts = new ArrayList<>();
        String pattern = "%" + escapeLike(value) + "%";
        for (String column : columns) {
            parts.add(column + " COLLATE " + SEARCH_COLLATION + " LIKE ?");
            params.add(pattern);
        }
        conditions.add("(" + String.join(" OR ", parts) + ")");
    }

    // Ký tự đại diện của LIKE trong SQL Server được bọc [] để tìm đúng chuỗi người dùng nhập
    private String escapeLike(String value) {
        return value.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setLogId(rs.getLong("LogID"));
        Timestamp created = rs.getTimestamp("CreatedAt");
        log.setCreatedAt(created == null ? null : created.toLocalDateTime());
        log.setLevel(rs.getString("LogLevel"));
        log.setEventType(rs.getString("EventType"));
        log.setAction(rs.getString("Action"));
        int userId = rs.getInt("UserID");
        log.setUserId(rs.wasNull() ? null : userId);
        log.setActorName(rs.getString("ActorName"));
        log.setActorEmail(rs.getString("ActorEmail"));
        log.setActorRole(rs.getString("ActorRole"));
        log.setTargetType(rs.getString("TargetType"));
        log.setTargetId(rs.getString("TargetID"));
        log.setDescription(rs.getString("Description"));
        log.setIpAddress(rs.getString("IpAddress"));
        return log;
    }
}
