package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.ReminderTicket;
import vn.edu.fpt.model.ShowtimeReminder;
import vn.edu.fpt.util.DBContext;

public class ShowtimeReminderDAO {
    private static final int MAX_ATTEMPTS_PER_RUN = 50;

    public void enqueueDueBookings() throws SQLException {
        String sql = "INSERT INTO BookingReminderEmail (BookingID) "
                + "SELECT DISTINCT b.BookingID FROM Booking b "
                + "JOIN Ticket t ON t.BookingID = b.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "WHERE b.Status = 'Paid' AND b.CustomerID IS NOT NULL AND t.Status = 'Upcoming' "
                + "AND s.StartTime > DATEADD(hour, 1, GETDATE()) "
                + "AND s.StartTime <= DATEADD(hour, 2, GETDATE()) "
                + "AND NOT EXISTS (SELECT 1 FROM BookingReminderEmail r WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE r.BookingID = b.BookingID)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public List<ShowtimeReminder> sendDueBatch() throws SQLException {
        List<ShowtimeReminder> reminders = new ArrayList<>();
        for (int i = 0; i < MAX_ATTEMPTS_PER_RUN; i++) {
            Integer bookingId = claimNextBooking();
            if (bookingId == null) {
                break;
            }
            ShowtimeReminder reminder = findReminder(bookingId);
            if (reminder == null) {
                markSent(bookingId);
            } else {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    public void markSent(int bookingId) throws SQLException {
        String sql = "UPDATE BookingReminderEmail SET SentAt = GETDATE(), LastError = NULL WHERE BookingID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    public void markFailed(int bookingId) throws SQLException {
        String sql = "UPDATE BookingReminderEmail SET LastError = ? WHERE BookingID = ? AND SentAt IS NULL";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Email delivery failed.");
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    private Integer claimNextBooking() throws SQLException {
        String sql = "UPDATE TOP (1) BookingReminderEmail WITH (UPDLOCK, READPAST, ROWLOCK) "
                + "SET AttemptCount = AttemptCount + 1, LastAttemptAt = GETDATE(), "
                + "NextAttemptAt = DATEADD(minute, 15, GETDATE()) "
                + "OUTPUT inserted.BookingID WHERE SentAt IS NULL AND AttemptCount < 3 "
                + "AND NextAttemptAt <= GETDATE()";
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                Integer bookingId = rs.next() ? rs.getInt(1) : null;
                conn.commit();
                return bookingId;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    private ShowtimeReminder findReminder(int bookingId) throws SQLException {
        String sql = "SELECT b.BookingID, u.Email, u.FullName, m.Title, s.StartTime, seat.RowName, seat.ColNumber, "
                + "t.TicketCode, t.QRCode FROM Booking b "
                + "JOIN [User] u ON u.UserID = b.CustomerID "
                + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movie m ON m.MovieID = s.MovieID "
                + "JOIN Ticket t ON t.BookingID = b.BookingID AND t.Status = 'Upcoming' "
                + "JOIN Seat seat ON seat.SeatID = t.SeatID "
                + "WHERE b.BookingID = ? AND b.Status = 'Paid' AND u.Email IS NOT NULL";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                ShowtimeReminder reminder = null;
                while (rs.next()) {
                    if (reminder == null) {
                        reminder = new ShowtimeReminder();
                        reminder.setBookingId(rs.getInt("BookingID"));
                        reminder.setRecipientEmail(rs.getString("Email"));
                        reminder.setRecipientName(rs.getString("FullName"));
                        reminder.setMovieTitle(rs.getString("Title"));
                        Timestamp start = rs.getTimestamp("StartTime");
                        reminder.setShowtimeStart(start == null ? null : start.toLocalDateTime());
                    }
                    ReminderTicket ticket = new ReminderTicket();
                    ticket.setSeatLabel(rs.getString("RowName") + rs.getInt("ColNumber"));
                    ticket.setTicketCode(rs.getString("TicketCode"));
                    ticket.setQrCode(rs.getString("QRCode"));
                    reminder.addTicket(ticket);
                }
                return reminder;
            }
        }
    }
}
