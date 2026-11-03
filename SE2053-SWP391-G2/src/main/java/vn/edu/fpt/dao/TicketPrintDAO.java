package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.PrintBooking;
import vn.edu.fpt.model.PrintTicket;
import vn.edu.fpt.util.DBContext;

// UC-36: tra cứu vé theo mã, đọc đơn / vé / bắp nước và ghi lần in vé giấy (bảng TicketPrint).
// Chỉ đọc Booking/Ticket/TicketExchange/Booking_Concession, không đổi trạng thái vé hay đơn.
public class TicketPrintDAO {

    // Mã vé (TK...) hoặc mã soát vé (Ticket.QRCode); 0 nếu không có
    public int findBookingIdByCode(String code) throws SQLException {
        String sql = "SELECT TOP 1 BookingID FROM Ticket WHERE TicketCode = ? OR QRCode = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int findBookingIdByTicket(int ticketId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT BookingID FROM Ticket WHERE TicketID = ?")) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Đơn kèm suất chiếu (lấy theo vé đầu tiên vì vé có thể mang ShowtimeID riêng), vé và bắp nước; null nếu không có vé
    public PrintBooking findBooking(int bookingId) throws SQLException {
        String sql = "SELECT TOP 1 b.BookingID, b.Status AS BookingStatus, b.Channel, c.FullName AS CustomerName, "
                + "s.ShowtimeID, s.Status AS ShowtimeStatus, s.StartTime, s.EndTime, m.Title, m.AgeRating, "
                + "r.RoomName, r.Format "
                + "FROM Booking b JOIN Ticket t ON t.BookingID = b.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = COALESCE(t.ShowtimeID, b.ShowtimeID) "
                + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
                + "LEFT JOIN [User] c ON c.UserID = b.CustomerID WHERE b.BookingID = ? ORDER BY t.TicketID";
        PrintBooking booking;
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                booking = new PrintBooking();
                booking.setBookingId(rs.getInt("BookingID"));
                booking.setBookingStatus(rs.getString("BookingStatus"));
                booking.setChannel(rs.getString("Channel"));
                booking.setCustomerName(rs.getString("CustomerName"));
                booking.setShowtimeId(rs.getInt("ShowtimeID"));
                booking.setShowtimeStatus(rs.getString("ShowtimeStatus"));
                booking.setStartTime(toLocal(rs.getTimestamp("StartTime")));
                booking.setEndTime(toLocal(rs.getTimestamp("EndTime")));
                booking.setMovieTitle(rs.getString("Title"));
                booking.setAgeRating(rs.getString("AgeRating"));
                booking.setRoomName(rs.getString("RoomName"));
                booking.setRoomFormat(rs.getString("Format"));
            }
        }
        booking.setTickets(findTickets(bookingId));
        booking.setConcessions(findConcessions(bookingId));
        return booking;
    }

    // Vé của đơn kèm lần in gần nhất và vé thay thế nếu đã đổi ghế
    private List<PrintTicket> findTickets(int bookingId) throws SQLException {
        String sql = "SELECT t.TicketID, CONCAT(se.RowName, se.ColNumber) AS SeatLabel, se.SeatType, t.Price, t.TicketCode, "
                + "t.Status, lp.PrintNo, lp.PrintedAt, lp.StaffName, "
                + "CONCAT(ns.RowName, ns.ColNumber) AS NewSeat, nt.TicketCode AS NewCode "
                + "FROM Ticket t JOIN Seat se ON se.SeatID = t.SeatID "
                + "OUTER APPLY (SELECT TOP 1 p.PrintNo, p.PrintedAt, u.FullName AS StaffName FROM TicketPrint p "
                + "JOIN [User] u ON u.UserID = p.StaffID WHERE p.TicketID = t.TicketID ORDER BY p.PrintNo DESC) lp "
                + "LEFT JOIN TicketExchange x ON x.OldTicketID = t.TicketID "
                + "LEFT JOIN Ticket nt ON nt.TicketID = x.NewTicketID LEFT JOIN Seat ns ON ns.SeatID = nt.SeatID "
                + "WHERE t.BookingID = ? ORDER BY se.RowName, se.ColNumber, t.TicketID";
        List<PrintTicket> tickets = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrintTicket ticket = new PrintTicket();
                    ticket.setTicketId(rs.getInt("TicketID"));
                    ticket.setSeatLabel(rs.getString("SeatLabel"));
                    ticket.setSeatType(rs.getString("SeatType"));
                    ticket.setPrice(rs.getBigDecimal("Price"));
                    ticket.setTicketCode(rs.getString("TicketCode"));
                    ticket.setStatus(rs.getString("Status"));
                    ticket.setPrintCount(rs.getInt("PrintNo"));
                    ticket.setPrintedAt(toLocal(rs.getTimestamp("PrintedAt")));
                    ticket.setPrintedBy(rs.getString("StaffName"));
                    String newCode = rs.getString("NewCode");
                    if (newCode != null) {
                        ticket.setExchangedCode(newCode);
                        ticket.setExchangedSeat(rs.getString("NewSeat"));
                    }
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    private List<String> findConcessions(int bookingId) throws SQLException {
        String sql = "SELECT c.Name, bc.Quantity FROM Booking_Concession bc JOIN Concession c ON c.ConcessionID = bc.ConcessionID "
                + "WHERE bc.BookingID = ? ORDER BY c.Name";
        List<String> items = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(rs.getString("Name") + " x " + rs.getInt("Quantity"));
                }
            }
        }
        return items;
    }

    // Đơn đã từng in vé giấy chưa (phiếu nhận bắp nước chỉ in kèm lần in đầu tiên của đơn)
    public boolean hasAnyPrint(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TicketPrint p JOIN Ticket t ON t.TicketID = p.TicketID WHERE t.BookingID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // PrintNo lớn nhất của vé, khóa dòng để hai lần in lại cùng lúc không lấy trùng số
    public int lockLastPrintNo(Connection conn, int ticketId) throws SQLException {
        String sql = "SELECT ISNULL(MAX(PrintNo), 0) FROM TicketPrint WITH (UPDLOCK, HOLDLOCK) WHERE TicketID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public LocalDateTime insertPrint(Connection conn, int ticketId, int printNo, String reason, int staffId, int shiftId)
            throws SQLException {
        String sql = "INSERT INTO TicketPrint (TicketID, PrintNo, Reason, StaffID, ShiftID) OUTPUT INSERTED.PrintedAt "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ps.setInt(2, printNo);
            ps.setString(3, reason);
            ps.setInt(4, staffId);
            ps.setInt(5, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toLocal(rs.getTimestamp(1)) : LocalDateTime.now();
            }
        }
    }

    private LocalDateTime toLocal(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
