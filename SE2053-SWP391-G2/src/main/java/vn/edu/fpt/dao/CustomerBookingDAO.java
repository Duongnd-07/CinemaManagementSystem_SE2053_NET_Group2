package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.BookingHistoryEntry;
import vn.edu.fpt.model.CancellableTicket;
import vn.edu.fpt.util.DBContext;

public class CustomerBookingDAO {
    private static final String HISTORY_FROM = " FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
            + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
            + "JOIN Movie m ON m.MovieID = s.MovieID "
            + "JOIN Seat seat ON seat.SeatID = t.SeatID WHERE b.CustomerID = ?";

    public int countHistory(int customerId) throws SQLException {
        String sql = "SELECT COUNT(*)" + HISTORY_FROM;
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<BookingHistoryEntry> findHistory(int customerId, int offset, int limit) throws SQLException {
        String sql = "SELECT b.BookingID, t.TicketID, m.Title, seat.RowName, seat.ColNumber, t.TicketCode, "
                + "t.QRCode, t.Status AS TicketStatus, b.Status AS BookingStatus, b.Channel, t.Price, b.FinalAmount, "
                + "b.BookingTime, s.StartTime" + HISTORY_FROM
                + " ORDER BY b.BookingTime DESC, b.BookingID DESC, t.TicketID DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<BookingHistoryEntry> entries = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingHistoryEntry entry = new BookingHistoryEntry();
                    entry.setBookingId(rs.getInt("BookingID"));
                    entry.setTicketId(rs.getInt("TicketID"));
                    entry.setMovieTitle(rs.getString("Title"));
                    entry.setSeatLabel(rs.getString("RowName") + rs.getInt("ColNumber"));
                    entry.setTicketCode(rs.getString("TicketCode"));
                    entry.setQrCode(rs.getString("QRCode"));
                    entry.setTicketStatus(rs.getString("TicketStatus"));
                    entry.setBookingStatus(rs.getString("BookingStatus"));
                    entry.setChannel(rs.getString("Channel"));
                    entry.setTicketPrice(rs.getBigDecimal("Price"));
                    entry.setFinalAmount(rs.getBigDecimal("FinalAmount"));
                    Timestamp bookingTime = rs.getTimestamp("BookingTime");
                    entry.setBookingTime(bookingTime == null ? null : bookingTime.toLocalDateTime());
                    Timestamp startTime = rs.getTimestamp("StartTime");
                    entry.setShowtimeStart(startTime == null ? null : startTime.toLocalDateTime());
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public CancellableTicket findCancellationCandidate(Connection conn, int ticketId, int customerId)
            throws SQLException {
        String sql = "SELECT t.TicketID, b.BookingID, t.SeatID, b.ShowtimeID, t.Price, b.PointsEarned, "
                + "(SELECT SUM(allTicket.Price) FROM Ticket allTicket WHERE allTicket.BookingID = b.BookingID) "
                + "AS BookingTicketTotal, "
                + "(SELECT COUNT(*) FROM Ticket upcoming WHERE upcoming.BookingID = b.BookingID "
                + "AND upcoming.Status = 'Upcoming') AS UpcomingTicketCount, "
                + "t.Status AS TicketStatus, b.Status AS BookingStatus, b.Channel, s.StartTime "
                + "FROM Ticket t WITH (UPDLOCK, ROWLOCK) "
                + "JOIN Booking b WITH (UPDLOCK, ROWLOCK) ON b.BookingID = t.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "WHERE t.TicketID = ? AND b.CustomerID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                CancellableTicket ticket = new CancellableTicket();
                ticket.setTicketId(rs.getInt("TicketID"));
                ticket.setBookingId(rs.getInt("BookingID"));
                ticket.setSeatId(rs.getInt("SeatID"));
                ticket.setShowtimeId(rs.getInt("ShowtimeID"));
                ticket.setPrice(rs.getBigDecimal("Price"));
                ticket.setBookingTicketTotal(rs.getBigDecimal("BookingTicketTotal"));
                ticket.setBookingPointsEarned(rs.getInt("PointsEarned"));
                ticket.setUpcomingTicketCount(rs.getInt("UpcomingTicketCount"));
                ticket.setTicketStatus(rs.getString("TicketStatus"));
                ticket.setBookingStatus(rs.getString("BookingStatus"));
                ticket.setChannel(rs.getString("Channel"));
                Timestamp startTime = rs.getTimestamp("StartTime");
                ticket.setShowtimeStart(startTime == null ? null : startTime.toLocalDateTime());
                return ticket;
            }
        }
    }

    public CancellableTicket findCancellationCandidate(int ticketId, int customerId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findCancellationCandidate(conn, ticketId, customerId);
        }
    }

    public int markTicketCancelled(Connection conn, int ticketId, java.math.BigDecimal refundAmount)
            throws SQLException {
        String sql = "UPDATE Ticket SET Status = 'Cancelled', RefundAmount = ? "
                + "WHERE TicketID = ? AND Status = 'Upcoming'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, refundAmount);
            ps.setInt(2, ticketId);
            return ps.executeUpdate();
        }
    }

    public int findCancellationVoucherDays(Connection conn) throws SQLException {
        String sql = "SELECT TRY_CONVERT(INT, ConfigValue) FROM SystemConfig "
                + "WHERE ConfigKey = 'CANCELLATION_VOUCHER_DAYS'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("CANCELLATION_VOUCHER_DAYS is not configured.");
            }
            int days = rs.getInt(1);
            if (rs.wasNull() || days < 1) {
                throw new SQLException("CANCELLATION_VOUCHER_DAYS must be a positive integer.");
            }
            return days;
        }
    }
}
