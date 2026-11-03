package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.RoomConditionItem;
import vn.edu.fpt.model.RoomConditionReport;
import vn.edu.fpt.util.DBContext;

// UC-35: bảng RoomConditionReport và RoomConditionItem (script UC35_room_condition_report.sql)
public class RoomConditionDAO {
    private static final String SELECT_COLUMNS =
            "SELECT r.ReportID, r.RoomID, rm.RoomName, r.StaffID, u.FullName, r.Result, r.Note, r.CreatedAt ";
    private static final String FROM_JOINS = "FROM RoomConditionReport r "
            + "JOIN Room rm ON rm.RoomID = r.RoomID JOIN [User] u ON u.UserID = r.StaffID ";

    // Giữ khóa tới hết transaction để hai lần gửi cùng lúc không cùng lọt qua
    public boolean existsForUpcomingShift(Connection conn, int staffId, int roomId) throws SQLException {
        String sql = "SELECT TOP 1 1 FROM RoomConditionReport WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE StaffID = ? AND RoomID = ? AND CreatedAt >= CAST(GETDATE() AS DATE) "
                + "AND CreatedAt >= COALESCE((SELECT MAX(EndedAt) FROM Shift WHERE StaffID = ?), '19000101')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setInt(2, roomId);
            ps.setInt(3, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection conn, RoomConditionReport report) throws SQLException {
        String sql = "INSERT INTO RoomConditionReport (RoomID, StaffID, Result, Note) VALUES (?, ?, ?, ?)";
        int reportId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, report.getRoomId());
            ps.setInt(2, report.getStaffId());
            ps.setString(3, report.getResult());
            ps.setString(4, report.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được mã báo cáo tình trạng phòng vừa tạo");
                }
                reportId = keys.getInt(1);
            }
        }
        String itemSql = "INSERT INTO RoomConditionItem (ReportID, ItemKey, IsOk, Note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            for (RoomConditionItem item : report.getItems()) {
                ps.setInt(1, reportId);
                ps.setString(2, item.getItemKey());
                ps.setBoolean(3, item.isOk());
                ps.setString(4, item.getNote());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return reportId;
    }

    public int count(Integer staffId, LocalDate date, String result) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM RoomConditionReport r " + where(staffId, date, result, params);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<RoomConditionReport> search(Integer staffId, LocalDate date, String result, int offset, int limit)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = SELECT_COLUMNS + FROM_JOINS + where(staffId, date, result, params)
                + "ORDER BY r.CreatedAt DESC, r.ReportID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        params.add(offset);
        params.add(limit);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            List<RoomConditionReport> reports = readReports(ps);
            loadItems(conn, reports);
            return reports;
        }
    }

    // Báo cáo mới nhất của từng phòng trong ngày; khóa là RoomID
    public Map<Integer, RoomConditionReport> findLatestByRoom(LocalDate date) throws SQLException {
        String sql = SELECT_COLUMNS + FROM_JOINS + "WHERE r.ReportID IN (SELECT ReportID FROM ("
                + "SELECT ReportID, ROW_NUMBER() OVER (PARTITION BY RoomID ORDER BY CreatedAt DESC, ReportID DESC) AS Rn "
                + "FROM RoomConditionReport WHERE CreatedAt >= ? AND CreatedAt < ?) latest WHERE Rn = 1)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date.plusDays(1)));
            Map<Integer, RoomConditionReport> latest = new LinkedHashMap<>();
            for (RoomConditionReport report : readReports(ps)) {
                latest.put(report.getRoomId(), report);
            }
            return latest;
        }
    }

    private String where(Integer staffId, LocalDate date, String result, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (staffId != null) {
            conditions.add("r.StaffID = ?");
            params.add(staffId);
        }
        if (date != null) {
            conditions.add("r.CreatedAt >= ? AND r.CreatedAt < ?");
            params.add(Date.valueOf(date));
            params.add(Date.valueOf(date.plusDays(1)));
        }
        if (result != null) {
            conditions.add("r.Result = ?");
            params.add(result);
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private List<RoomConditionReport> readReports(PreparedStatement ps) throws SQLException {
        List<RoomConditionReport> reports = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RoomConditionReport report = new RoomConditionReport();
                report.setReportId(rs.getInt("ReportID"));
                report.setRoomId(rs.getInt("RoomID"));
                report.setRoomName(rs.getString("RoomName"));
                report.setStaffId(rs.getInt("StaffID"));
                report.setStaffName(rs.getString("FullName"));
                report.setResult(rs.getString("Result"));
                report.setNote(rs.getString("Note"));
                Timestamp createdAt = rs.getTimestamp("CreatedAt");
                report.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                reports.add(report);
            }
        }
        return reports;
    }

    private void loadItems(Connection conn, List<RoomConditionReport> reports) throws SQLException {
        if (reports.isEmpty()) {
            return;
        }
        Map<Integer, RoomConditionReport> byId = new LinkedHashMap<>();
        for (RoomConditionReport report : reports) {
            byId.put(report.getReportId(), report);
        }
        String sql = "SELECT ReportID, ItemKey, IsOk, Note FROM RoomConditionItem WHERE ReportID IN ("
                + String.join(",", Collections.nCopies(byId.size(), "?")) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (int reportId : byId.keySet()) {
                ps.setInt(index++, reportId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byId.get(rs.getInt("ReportID")).getItems().add(new RoomConditionItem(
                            rs.getString("ItemKey"), rs.getBoolean("IsOk"), rs.getString("Note")));
                }
            }
        }
    }
}
