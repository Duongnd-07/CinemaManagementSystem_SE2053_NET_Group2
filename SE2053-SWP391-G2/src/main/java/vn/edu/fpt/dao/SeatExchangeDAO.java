package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.service.ExchangeRecord;
import vn.edu.fpt.service.ExchangeTicket;
import vn.edu.fpt.util.DBContext;

// UC-20: truy vấn riêng của đổi ghế hỏng. Tạo vé, điểm dùng chung BookingDAO; báo cáo sự cố dùng EquipmentReportDAO
public class SeatExchangeDAO {

    private static final String TICKET_COLUMNS = "SELECT t.TicketID, t.TicketCode, t.Status AS TicketStatus, t.Price, "
            + "t.SeatID, se.RowName, se.ColNumber, se.SeatType, b.BookingID, b.Status AS BookingStatus, b.Channel, "
            + "b.TotalPrice, b.FinalAmount, b.PointsRedeemed, b.CustomerID, c.FullName AS CustomerName, "
            + "s.ShowtimeID, s.Status AS ShowtimeStatus, s.StartTime, s.EndTime, s.BasePrice, m.Title, "
            + "r.RoomID, r.RoomName ";
    private static final String TICKET_JOINS = "JOIN Booking b ON b.BookingID = t.BookingID "
            + "JOIN Seat se ON se.SeatID = t.SeatID "
            + "JOIN Showtime s ON s.ShowtimeID = COALESCE(t.ShowtimeID, b.ShowtimeID) "
            + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
            + "LEFT JOIN [User] c ON c.UserID = b.CustomerID ";

    public ExchangeTicket findTicketByCode(String ticketCode) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findTicket(conn, "t.TicketCode = ?", ticketCode, false);
        }
    }

    // lock = true trong transaction đổi ghế: hai nhân viên không cùng xử lý một vé
    public ExchangeTicket findTicketById(Connection conn, int ticketId, boolean lock) throws SQLException {
        return findTicket(conn, "t.TicketID = ?", ticketId, lock);
    }

    // Vé còn hiệu lực của suất chưa kết thúc đang nằm trên ghế bảo trì (ghế đã báo hỏng ở UC-21)
    public List<ExchangeTicket> findAffected() throws SQLException {
        String sql = TICKET_COLUMNS + "FROM Ticket t " + TICKET_JOINS
                + "WHERE t.Status = ? AND b.Status = ? AND se.Status = ? AND s.EndTime > GETDATE() "
                + "ORDER BY s.StartTime, r.RoomName, se.RowName, se.ColNumber";
        List<ExchangeTicket> tickets = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TicketStatus.UPCOMING);
            ps.setString(2, BookingStatus.PAID);
            ps.setString(3, RoomStatus.MAINTENANCE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapTicket(rs));
                }
            }
        }
        return tickets;
    }

    // Ghế đã có vé còn hiệu lực hoặc đang được khách online giữ trong suất chiếu
    public Set<Integer> findTakenSeatIds(Connection conn, int showtimeId) throws SQLException {
        String sql = "SELECT SeatID FROM Ticket WHERE ShowtimeID = ? AND Status IN (?, ?) "
                + "UNION SELECT SeatID FROM Seat_Hold WHERE ShowtimeID = ? AND ExpiryTime > GETDATE()";
        Set<Integer> taken = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setString(2, TicketStatus.UPCOMING);
            ps.setString(3, TicketStatus.WATCHED);
            ps.setInt(4, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    taken.add(rs.getInt(1));
                }
            }
        }
        return taken;
    }

    // Chỉ đổi khi vé vẫn còn hiệu lực; false nếu đã bị người khác xử lý
    public boolean closeTicket(Connection conn, int ticketId, String newStatus, BigDecimal refundAmount,
                               Integer refundShiftId) throws SQLException {
        String sql = "UPDATE Ticket SET Status = ?, RefundAmount = ?, RefundShiftID = ? WHERE TicketID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setBigDecimal(2, refundAmount);
            setNullableInt(ps, 3, refundShiftId);
            ps.setInt(4, ticketId);
            ps.setString(5, TicketStatus.UPCOMING);
            return ps.executeUpdate() > 0;
        }
    }

    // Báo cáo sự cố đang mở của ghế; khóa để không tạo trùng khi hai nhân viên xử lý cùng ghế
    public Integer findOpenSeatReport(Connection conn, int seatId) throws SQLException {
        String sql = "SELECT TOP 1 ReportID FROM Equipment_Report WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE SeatID = ? AND Status <> ? ORDER BY ReportID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            ps.setString(2, ReportStatus.RESOLVED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public Integer findVoucherId(Connection conn, String voucherCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT VoucherID FROM Voucher WHERE VoucherCode = ?")) {
            ps.setString(1, voucherCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    public int insertExchange(Connection conn, int oldTicketId, String type, Integer newTicketId, Integer newTicketId2,
                              String reason, BigDecimal cashRefund, int pointsReturned, Integer voucherId,
                              int staffId, Integer shiftId) throws SQLException {
        String sql = "INSERT INTO TicketExchange (OldTicketID, ExchangeType, NewTicketID, NewTicketID2, Reason, "
                + "CashRefund, PointsReturned, VoucherID, StaffID, ShiftID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, oldTicketId);
            ps.setString(2, type);
            setNullableInt(ps, 3, newTicketId);
            setNullableInt(ps, 4, newTicketId2);
            ps.setString(5, reason);
            ps.setBigDecimal(6, cashRefund);
            ps.setInt(7, pointsReturned);
            setNullableInt(ps, 8, voucherId);
            ps.setInt(9, staffId);
            setNullableInt(ps, 10, shiftId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được ExchangeID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    public ExchangeRecord findRecord(int exchangeId) throws SQLException {
        String sql = "SELECT x.ExchangeID, x.ExchangeType, x.Reason, x.CashRefund, x.PointsReturned, x.CreatedAt, "
                + "v.VoucherCode, v.DiscountValue, v.ExpiryDate, st.FullName AS StaffName, c.FullName AS CustomerName, "
                + "m.Title, r.RoomName, s.StartTime, ot.TicketCode AS OldCode, ose.RowName AS OldRow, ose.ColNumber AS OldCol, "
                + "n1.TicketCode AS Code1, s1.RowName AS Row1, s1.ColNumber AS Col1, "
                + "n2.TicketCode AS Code2, s2.RowName AS Row2, s2.ColNumber AS Col2 "
                + "FROM TicketExchange x JOIN Ticket ot ON ot.TicketID = x.OldTicketID "
                + "JOIN Seat ose ON ose.SeatID = ot.SeatID JOIN Booking b ON b.BookingID = ot.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = COALESCE(ot.ShowtimeID, b.ShowtimeID) "
                + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
                + "JOIN [User] st ON st.UserID = x.StaffID LEFT JOIN [User] c ON c.UserID = b.CustomerID "
                + "LEFT JOIN Voucher v ON v.VoucherID = x.VoucherID "
                + "LEFT JOIN Ticket n1 ON n1.TicketID = x.NewTicketID LEFT JOIN Seat s1 ON s1.SeatID = n1.SeatID "
                + "LEFT JOIN Ticket n2 ON n2.TicketID = x.NewTicketID2 LEFT JOIN Seat s2 ON s2.SeatID = n2.SeatID "
                + "WHERE x.ExchangeID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, exchangeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ExchangeRecord record = new ExchangeRecord();
                record.setExchangeId(rs.getInt("ExchangeID"));
                record.setExchangeType(rs.getString("ExchangeType"));
                record.setReason(rs.getString("Reason"));
                record.setCashRefund(rs.getBigDecimal("CashRefund"));
                record.setPointsReturned(rs.getInt("PointsReturned"));
                record.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
                record.setVoucherCode(rs.getString("VoucherCode"));
                record.setVoucherValue(rs.getBigDecimal("DiscountValue"));
                Timestamp expiry = rs.getTimestamp("ExpiryDate");
                record.setVoucherExpiry(expiry == null ? null : expiry.toLocalDateTime());
                record.setStaffName(rs.getString("StaffName"));
                record.setCustomerName(rs.getString("CustomerName"));
                record.setMovieTitle(rs.getString("Title"));
                record.setRoomName(rs.getString("RoomName"));
                record.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
                record.setOldTicketCode(rs.getString("OldCode"));
                record.setOldSeat(rs.getString("OldRow") + rs.getInt("OldCol"));
                if (rs.getString("Code1") != null) {
                    record.getNewTickets().add(new String[]{rs.getString("Row1") + rs.getInt("Col1"), rs.getString("Code1")});
                }
                if (rs.getString("Code2") != null) {
                    record.getNewTickets().add(new String[]{rs.getString("Row2") + rs.getInt("Col2"), rs.getString("Code2")});
                }
                return record;
            }
        }
    }

    private ExchangeTicket findTicket(Connection conn, String condition, Object value, boolean lock)
            throws SQLException {
        String sql = TICKET_COLUMNS + "FROM Ticket t " + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "") + TICKET_JOINS
                + "WHERE " + condition;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTicket(rs) : null;
            }
        }
    }

    private ExchangeTicket mapTicket(ResultSet rs) throws SQLException {
        ExchangeTicket ticket = new ExchangeTicket();
        ticket.setTicketId(rs.getInt("TicketID"));
        ticket.setTicketCode(rs.getString("TicketCode"));
        ticket.setTicketStatus(rs.getString("TicketStatus"));
        ticket.setPrice(rs.getBigDecimal("Price"));
        ticket.setSeatId(rs.getInt("SeatID"));
        ticket.setSeatLabel(rs.getString("RowName") + rs.getInt("ColNumber"));
        ticket.setSeatType(rs.getString("SeatType"));
        ticket.setBookingId(rs.getInt("BookingID"));
        ticket.setBookingStatus(rs.getString("BookingStatus"));
        ticket.setChannel(rs.getString("Channel"));
        ticket.setTotalPrice(rs.getBigDecimal("TotalPrice"));
        ticket.setFinalAmount(rs.getBigDecimal("FinalAmount"));
        ticket.setPointsRedeemed(rs.getInt("PointsRedeemed"));
        ticket.setCustomerId(rs.getObject("CustomerID", Integer.class));
        ticket.setCustomerName(rs.getString("CustomerName"));
        ticket.setShowtimeId(rs.getInt("ShowtimeID"));
        ticket.setShowtimeStatus(rs.getString("ShowtimeStatus"));
        ticket.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
        Timestamp end = rs.getTimestamp("EndTime");
        ticket.setEndTime(end == null ? null : end.toLocalDateTime());
        ticket.setBasePrice(rs.getBigDecimal("BasePrice"));
        ticket.setMovieTitle(rs.getString("Title"));
        ticket.setRoomId(rs.getInt("RoomID"));
        ticket.setRoomName(rs.getString("RoomName"));
        return ticket;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
