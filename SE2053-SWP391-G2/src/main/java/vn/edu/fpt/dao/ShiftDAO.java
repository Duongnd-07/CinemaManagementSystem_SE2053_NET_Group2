package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.util.DBContext;

// UC-19/UC-22: ca làm việc tại quầy. Mỗi nhân viên chỉ một ca đang mở (UX_Shift_OpenPerStaff)
public class ShiftDAO {

    private static final String SELECT_COLUMNS = "SELECT ShiftID, StaffID, CounterNo, StartedAt, EndedAt FROM Shift ";

    public Shift findOpenByStaff(int staffId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findOpenByStaff(conn, staffId, false);
        }
    }

    // lock = true khi gọi trong transaction bán vé để ca không bị đóng giữa chừng
    public Shift findOpenByStaff(Connection conn, int staffId, boolean lock) throws SQLException {
        String sql = SELECT_COLUMNS + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "") + "WHERE StaffID = ? AND EndedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void open(int staffId, String counterNo) throws SQLException {
        String sql = "INSERT INTO Shift (StaffID, CounterNo, StartedAt) VALUES (?, ?, GETDATE())";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, counterNo);
            ps.executeUpdate();
        }
    }

    // Chỉ ghi giờ kết ca; số liệu chốt (TicketCount, CashTotal...) do báo cáo ca UC-22 tính
    public boolean close(int staffId) throws SQLException {
        String sql = "UPDATE Shift SET EndedAt = GETDATE() WHERE StaffID = ? AND EndedAt IS NULL";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            return ps.executeUpdate() > 0;
        }
    }

    private Shift mapRow(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.setShiftId(rs.getInt("ShiftID"));
        shift.setStaffId(rs.getInt("StaffID"));
        shift.setCounterNo(rs.getString("CounterNo"));
        Timestamp started = rs.getTimestamp("StartedAt");
        shift.setStartedAt(started == null ? null : started.toLocalDateTime());
        Timestamp ended = rs.getTimestamp("EndedAt");
        shift.setEndedAt(ended == null ? null : ended.toLocalDateTime());
        return shift;
    }
}
