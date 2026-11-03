package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.InspectionSlot;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;

// UC-35: suất chiếu kèm lần kiểm tra phòng, ghi/sửa/hủy lần kiểm tra và gắn báo cáo sự cố (UC-21).
// Chỉ đọc Showtime/Room/Movie/Equipment_Report/Ticket, không sửa dữ liệu của các UC đó.
public class RoomInspectionDAO {
    // PrevEnd: suất trước gần nhất trong cùng phòng, cùng ngày, chưa hủy (mở khung giờ kiểm tra - BR-35-02)
    private static final String SLOT_SELECT = "SELECT s.ShowtimeID, s.RoomID, r.RoomName, m.Title, s.StartTime, s.EndTime, "
            + "s.Status AS ShowtimeStatus, "
            + "(SELECT MAX(p.EndTime) FROM Showtime p WHERE p.RoomID = s.RoomID AND p.ShowtimeID <> s.ShowtimeID "
            + "AND p.Status <> ? AND p.EndTime <= s.StartTime "
            + "AND CAST(p.StartTime AS DATE) = CAST(s.StartTime AS DATE)) AS PrevEnd, "
            + "i.InspectionID, i.StaffID, u.FullName AS StaffName, i.SeatsChecked, i.RoomCleaned, i.EquipmentOk, "
            + "i.HasIssue, i.IsLate, i.Note, i.InspectedAt, i.UpdatedAt, "
            + "(SELECT COUNT(*) FROM RoomInspectionReport x WHERE x.InspectionID = i.InspectionID) AS ReportCount "
            + "FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID JOIN Movie m ON m.MovieID = s.MovieID "
            + "LEFT JOIN RoomInspection i ON i.ShowtimeID = s.ShowtimeID "
            + "LEFT JOIN [User] u ON u.UserID = i.StaffID ";

    // Suất chiếu chưa hủy bắt đầu trong [from, to), lọc theo phòng nếu roomId > 0
    public List<InspectionSlot> findSlots(LocalDateTime from, LocalDateTime to, int roomId) throws SQLException {
        String sql = SLOT_SELECT + "WHERE s.StartTime >= ? AND s.StartTime < ? AND s.Status <> ? "
                + (roomId > 0 ? "AND s.RoomID = ? " : "") + "ORDER BY s.StartTime, r.RoomName";
        List<InspectionSlot> slots = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ShowtimeStatus.CANCELLED);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            ps.setString(4, ShowtimeStatus.CANCELLED);
            if (roomId > 0) {
                ps.setInt(5, roomId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapSlot(rs));
                }
            }
        }
        return slots;
    }

    public InspectionSlot findSlot(int showtimeId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(SLOT_SELECT + "WHERE s.ShowtimeID = ?")) {
            ps.setString(1, ShowtimeStatus.CANCELLED);
            ps.setInt(2, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapSlot(rs) : null;
            }
        }
    }

    public int insert(Connection conn, InspectionSlot slot, Integer shiftId) throws SQLException {
        String sql = "INSERT INTO RoomInspection (ShowtimeID, StaffID, ShiftID, SeatsChecked, RoomCleaned, EquipmentOk, "
                + "HasIssue, Note, IsLate, InspectedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, slot.getShowtimeId());
            ps.setInt(2, slot.getStaffId());
            if (shiftId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, shiftId);
            }
            ps.setBoolean(4, slot.isSeatsChecked());
            ps.setBoolean(5, slot.isRoomCleaned());
            ps.setBoolean(6, slot.isEquipmentOk());
            ps.setBoolean(7, slot.isHasIssue());
            ps.setString(8, slot.getNote());
            ps.setBoolean(9, slot.isLate());
            ps.setTimestamp(10, Timestamp.valueOf(slot.getInspectedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được InspectionID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    // Chỉ sửa khi vẫn là người lưu và còn trong hạn sửa (BR-35-07); trả về false nếu điều kiện không còn đúng
    public boolean update(Connection conn, InspectionSlot slot, LocalDateTime editableSince) throws SQLException {
        String sql = "UPDATE RoomInspection SET SeatsChecked = ?, RoomCleaned = ?, EquipmentOk = ?, HasIssue = ?, Note = ?, "
                + "UpdatedAt = GETDATE() WHERE InspectionID = ? AND StaffID = ? AND InspectedAt >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, slot.isSeatsChecked());
            ps.setBoolean(2, slot.isRoomCleaned());
            ps.setBoolean(3, slot.isEquipmentOk());
            ps.setBoolean(4, slot.isHasIssue());
            ps.setString(5, slot.getNote());
            ps.setInt(6, slot.getInspectionId());
            ps.setInt(7, slot.getStaffId());
            ps.setTimestamp(8, Timestamp.valueOf(editableSince));
            return ps.executeUpdate() > 0;
        }
    }

    // Hoàn tác: xóa lần kiểm tra (liên kết báo cáo xóa theo ON DELETE CASCADE, báo cáo UC-21 vẫn giữ)
    public boolean delete(int inspectionId, int staffId, LocalDateTime editableSince) throws SQLException {
        String sql = "DELETE FROM RoomInspection WHERE InspectionID = ? AND StaffID = ? AND InspectedAt >= ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inspectionId);
            ps.setInt(2, staffId);
            ps.setTimestamp(3, Timestamp.valueOf(editableSince));
            return ps.executeUpdate() > 0;
        }
    }

    // Báo cáo sự cố nhân viên này gửi cho phòng từ lúc mở khung kiểm tra, chưa gắn với lần kiểm tra nào khác
    public List<EquipmentReport> findNewReports(int roomId, int staffId, LocalDateTime since, int inspectionId)
            throws SQLException {
        String sql = REPORT_SELECT + "WHERE e.RoomID = ? AND e.StaffID = ? AND e.ReportDate >= ? "
                + "AND NOT EXISTS (SELECT 1 FROM RoomInspectionReport x WHERE x.ReportID = e.ReportID AND x.InspectionID <> ?) "
                + "ORDER BY e.ReportDate";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setInt(2, staffId);
            ps.setTimestamp(3, Timestamp.valueOf(since));
            ps.setInt(4, inspectionId);
            return readReports(ps);
        }
    }

    public List<EquipmentReport> findLinkedReports(int inspectionId) throws SQLException {
        String sql = REPORT_SELECT + "JOIN RoomInspectionReport x ON x.ReportID = e.ReportID WHERE x.InspectionID = ? "
                + "ORDER BY e.ReportDate";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inspectionId);
            return readReports(ps);
        }
    }

    public void linkReports(Connection conn, int inspectionId, List<EquipmentReport> reports) throws SQLException {
        if (reports.isEmpty()) {
            return;
        }
        String sql = "IF NOT EXISTS (SELECT 1 FROM RoomInspectionReport WHERE InspectionID = ? AND ReportID = ?) "
                + "INSERT INTO RoomInspectionReport (InspectionID, ReportID) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (EquipmentReport report : reports) {
                ps.setInt(1, inspectionId);
                ps.setInt(2, report.getReportId());
                ps.setInt(3, inspectionId);
                ps.setInt(4, report.getReportId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Số vé còn hiệu lực cho suất chưa kết thúc trên các ghế bị báo hỏng (để chuyển sang UC-20 Đổi ghế hỏng)
    public int countUpcomingTicketsOnSeats(List<Integer> seatIds) throws SQLException {
        if (seatIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(", ", java.util.Collections.nCopies(seatIds.size(), "?"));
        String sql = "SELECT COUNT(*) FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = COALESCE(t.ShowtimeID, b.ShowtimeID) "
                + "WHERE t.SeatID IN (" + placeholders + ") AND t.Status = ? AND b.Status = ? AND s.Status = ? "
                + "AND s.EndTime > GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (int seatId : seatIds) {
                ps.setInt(index++, seatId);
            }
            ps.setString(index++, TicketStatus.UPCOMING);
            ps.setString(index++, BookingStatus.PAID);
            ps.setString(index, ShowtimeStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static final String REPORT_SELECT = "SELECT e.ReportID, e.StaffID, e.RoomID, e.EquipmentCategory, e.Description, "
            + "e.UrgencyLevel, e.Status, e.ReportDate, e.SeatID, "
            + "CASE WHEN se.SeatID IS NULL THEN NULL ELSE CONCAT(se.RowName, se.ColNumber) END AS SeatLabel "
            + "FROM Equipment_Report e LEFT JOIN Seat se ON se.SeatID = e.SeatID ";

    private List<EquipmentReport> readReports(PreparedStatement ps) throws SQLException {
        List<EquipmentReport> reports = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                EquipmentReport report = new EquipmentReport();
                report.setReportId(rs.getInt("ReportID"));
                report.setStaffId(rs.getInt("StaffID"));
                report.setRoomId(rs.getInt("RoomID"));
                report.setEquipmentCategory(rs.getString("EquipmentCategory"));
                report.setDescription(rs.getString("Description"));
                report.setUrgencyLevel(rs.getString("UrgencyLevel"));
                report.setStatus(rs.getString("Status"));
                report.setReportDate(toLocal(rs.getTimestamp("ReportDate")));
                int seatId = rs.getInt("SeatID");
                report.setSeatId(rs.wasNull() ? null : seatId);
                report.setSeatLabel(rs.getString("SeatLabel"));
                reports.add(report);
            }
        }
        return reports;
    }

    private InspectionSlot mapSlot(ResultSet rs) throws SQLException {
        InspectionSlot slot = new InspectionSlot();
        slot.setShowtimeId(rs.getInt("ShowtimeID"));
        slot.setRoomId(rs.getInt("RoomID"));
        slot.setRoomName(rs.getString("RoomName"));
        slot.setMovieTitle(rs.getString("Title"));
        slot.setStartTime(toLocal(rs.getTimestamp("StartTime")));
        slot.setEndTime(toLocal(rs.getTimestamp("EndTime")));
        slot.setShowtimeStatus(rs.getString("ShowtimeStatus"));
        slot.setPreviousEnd(toLocal(rs.getTimestamp("PrevEnd")));
        slot.setInspectionId(rs.getInt("InspectionID"));
        if (slot.getInspectionId() > 0) {
            slot.setStaffId(rs.getInt("StaffID"));
            slot.setStaffName(rs.getString("StaffName"));
            slot.setSeatsChecked(rs.getBoolean("SeatsChecked"));
            slot.setRoomCleaned(rs.getBoolean("RoomCleaned"));
            slot.setEquipmentOk(rs.getBoolean("EquipmentOk"));
            slot.setHasIssue(rs.getBoolean("HasIssue"));
            slot.setLate(rs.getBoolean("IsLate"));
            slot.setNote(rs.getString("Note"));
            slot.setInspectedAt(toLocal(rs.getTimestamp("InspectedAt")));
            slot.setUpdatedAt(toLocal(rs.getTimestamp("UpdatedAt")));
            slot.setReportCount(rs.getInt("ReportCount"));
        }
        return slot;
    }

    private LocalDateTime toLocal(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
