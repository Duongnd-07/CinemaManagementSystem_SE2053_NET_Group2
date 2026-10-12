package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.util.DBContext;

public class EquipmentReportDAO {

    private static final String SELECT_WITH_PLACE = "SELECT r.ReportID, r.StaffID, r.RoomID, r.SeatID, "
            + "r.EquipmentCategory, r.Description, r.UrgencyLevel, r.Status, r.ReportDate, r.ResolvedDate, "
            + "rm.RoomName, s.RowName, s.ColNumber "
            + "FROM Equipment_Report r "
            + "JOIN Room rm ON rm.RoomID = r.RoomID "
            + "LEFT JOIN Seat s ON s.SeatID = r.SeatID ";

    public int insert(Connection conn, EquipmentReport report) throws SQLException {
        String sql = "INSERT INTO Equipment_Report (StaffID, RoomID, SeatID, EquipmentCategory, Description, "
                + "UrgencyLevel, Status, ReportDate) VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, report.getStaffId());
            ps.setInt(2, report.getRoomId());
            if (report.getSeatId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, report.getSeatId());
            }
            ps.setString(4, report.getEquipmentCategory());
            ps.setString(5, report.getDescription());
            ps.setString(6, report.getUrgencyLevel());
            ps.setString(7, report.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public List<EquipmentReport> searchByStaff(int staffId, String status, int offset, int limit)
            throws SQLException {
        String sql = SELECT_WITH_PLACE + buildStaffWhere(status)
                + "ORDER BY r.ReportDate DESC, r.ReportID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<EquipmentReport> reports = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindStaffFilter(ps, staffId, status);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapRow(rs));
                }
            }
        }
        return reports;
    }

    public int countByStaff(int staffId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Equipment_Report r " + buildStaffWhere(status);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindStaffFilter(ps, staffId, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> countByStatusForStaff(int staffId) throws SQLException {
        String sql = "SELECT Status, COUNT(*) AS Total FROM Equipment_Report WHERE StaffID = ? GROUP BY Status";
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return result;
    }

    private String buildStaffWhere(String status) {
        return status == null || status.isEmpty() ? "WHERE r.StaffID = ? " : "WHERE r.StaffID = ? AND r.Status = ? ";
    }

    private int bindStaffFilter(PreparedStatement ps, int staffId, String status) throws SQLException {
        int idx = 1;
        ps.setInt(idx++, staffId);
        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }
        return idx;
    }

    private EquipmentReport mapRow(ResultSet rs) throws SQLException {
        EquipmentReport report = new EquipmentReport();
        report.setReportId(rs.getInt("ReportID"));
        report.setStaffId(rs.getInt("StaffID"));
        report.setRoomId(rs.getInt("RoomID"));
        report.setSeatId(rs.getObject("SeatID", Integer.class));
        report.setEquipmentCategory(rs.getString("EquipmentCategory"));
        report.setDescription(rs.getString("Description"));
        report.setUrgencyLevel(rs.getString("UrgencyLevel"));
        report.setStatus(rs.getString("Status"));
        Timestamp reportDate = rs.getTimestamp("ReportDate");
        report.setReportDate(reportDate == null ? null : reportDate.toLocalDateTime());
        Timestamp resolvedDate = rs.getTimestamp("ResolvedDate");
        report.setResolvedDate(resolvedDate == null ? null : resolvedDate.toLocalDateTime());
        report.setRoomName(rs.getString("RoomName"));
        String rowName = rs.getString("RowName");
        report.setSeatLabel(rowName == null ? null : rowName + rs.getInt("ColNumber"));
        return report;
    }
}
