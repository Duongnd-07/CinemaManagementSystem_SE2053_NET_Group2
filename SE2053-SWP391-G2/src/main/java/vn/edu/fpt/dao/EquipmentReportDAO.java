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
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.util.DBContext;

public class EquipmentReportDAO {

    private static final String SELECT_WITH_PLACE = "SELECT r.ReportID, r.StaffID, r.RoomID, r.SeatID, "
            + "r.EquipmentCategory, r.Description, r.UrgencyLevel, r.Status, r.ReportDate, r.ResolvedDate, "
            + "rm.RoomName, rm.Status AS RoomStatus, s.RowName, s.ColNumber, u.FullName AS StaffName "
            + "FROM Equipment_Report r "
            + "JOIN Room rm ON rm.RoomID = r.RoomID "
            + "LEFT JOIN Seat s ON s.SeatID = r.SeatID "
            + "LEFT JOIN [User] u ON u.UserID = r.StaffID ";

    // UC-26: việc chưa xong đứng trước, trong cùng trạng thái thì khẩn cấp hơn đứng trước
    private static final String ORDER_FOR_MANAGER = "ORDER BY "
            + "CASE r.Status WHEN 'Pending' THEN 0 WHEN 'In Progress' THEN 1 ELSE 2 END, "
            + "CASE r.UrgencyLevel WHEN 'Critical' THEN 0 WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 ELSE 3 END, "
            + "r.ReportDate DESC, r.ReportID DESC ";

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
        return search(staffId, status, "ORDER BY r.ReportDate DESC, r.ReportID DESC ", offset, limit);
    }

    public int countByStaff(int staffId, String status) throws SQLException {
        return count(staffId, status);
    }

    public Map<String, Integer> countByStatusForStaff(int staffId) throws SQLException {
        return countByStatus(staffId);
    }

    public List<EquipmentReport> searchAll(String status, int offset, int limit) throws SQLException {
        return search(null, status, ORDER_FOR_MANAGER, offset, limit);
    }

    public int countAll(String status) throws SQLException {
        return count(null, status);
    }

    public Map<String, Integer> countAllByStatus() throws SQLException {
        return countByStatus(null);
    }

    public EquipmentReport findById(int reportId) throws SQLException {
        String sql = SELECT_WITH_PLACE + "WHERE r.ReportID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // Báo cáo chưa xử lý xong của mọi nhân viên, dùng để cảnh báo trùng khi gửi báo cáo mới
    public List<EquipmentReport> findOpen() throws SQLException {
        String sql = SELECT_WITH_PLACE + "WHERE r.Status <> ? " + ORDER_FOR_MANAGER;
        List<EquipmentReport> reports = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ReportStatus.RESOLVED);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapRow(rs));
                }
            }
        }
        return reports;
    }

    // Mã báo cáo chưa xử lý xong của ghế, null nếu không có
    public Integer findOpenReportIdBySeat(int seatId) throws SQLException {
        String sql = "SELECT TOP 1 ReportID FROM Equipment_Report WHERE SeatID = ? AND Status <> ? ORDER BY ReportID";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            ps.setString(2, ReportStatus.RESOLVED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // Chỉ cập nhật khi trạng thái hiện tại vẫn là expectedStatus; trả về false nếu đã có người đổi trước
    public boolean updateStatus(Connection conn, int reportId, String expectedStatus, String newStatus)
            throws SQLException {
        String sql = "UPDATE Equipment_Report SET Status = ?, "
                + "ResolvedDate = CASE WHEN ? = 1 THEN GETDATE() ELSE ResolvedDate END "
                + "WHERE ReportID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, ReportStatus.RESOLVED.equals(newStatus) ? 1 : 0);
            ps.setInt(3, reportId);
            ps.setString(4, expectedStatus);
            return ps.executeUpdate() > 0;
        }
    }

    // Số báo cáo khác chưa xử lý xong của cùng một ghế
    public int countOtherOpenBySeat(Connection conn, int seatId, int excludeReportId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Equipment_Report WHERE SeatID = ? AND ReportID <> ? AND Status <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            ps.setInt(2, excludeReportId);
            ps.setString(3, ReportStatus.RESOLVED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // staffId null nghĩa là lấy báo cáo của mọi nhân viên
    private List<EquipmentReport> search(Integer staffId, String status, String orderBy, int offset, int limit)
            throws SQLException {
        String sql = SELECT_WITH_PLACE + buildWhere(staffId, status) + orderBy
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<EquipmentReport> reports = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilter(ps, staffId, status);
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

    private int count(Integer staffId, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Equipment_Report r " + buildWhere(staffId, status);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, staffId, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Map<String, Integer> countByStatus(Integer staffId) throws SQLException {
        String sql = "SELECT r.Status, COUNT(*) AS Total FROM Equipment_Report r " + buildWhere(staffId, null)
                + "GROUP BY r.Status";
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, staffId, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return result;
    }

    private String buildWhere(Integer staffId, String status) {
        List<String> conditions = new ArrayList<>();
        if (staffId != null) {
            conditions.add("r.StaffID = ?");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("r.Status = ?");
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private int bindFilter(PreparedStatement ps, Integer staffId, String status) throws SQLException {
        int idx = 1;
        if (staffId != null) {
            ps.setInt(idx++, staffId);
        }
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
        report.setRoomStatus(rs.getString("RoomStatus"));
        String rowName = rs.getString("RowName");
        report.setSeatLabel(rowName == null ? null : rowName + rs.getInt("ColNumber"));
        report.setStaffName(rs.getString("StaffName"));
        return report;
    }
}
