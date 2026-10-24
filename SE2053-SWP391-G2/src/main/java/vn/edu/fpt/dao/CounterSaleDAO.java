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
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.service.CounterReceipt;
import vn.edu.fpt.util.DBContext;

// UC-19: truy vấn riêng của bán vé tại quầy. Vé, bắp nước, điểm, voucher dùng chung BookingDAO (UC-13)
public class CounterSaleDAO {

    // Suất còn mở bán trong khoảng [from, to): suất Active chưa bắt đầu, phim công khai, phòng đang hoạt động
    public List<Showtime> findSellableShowtimes(LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.StartTime, s.EndTime, s.BasePrice, s.Status, "
                + "s.TranslationType, m.Title, m.AgeRating, r.RoomName, r.Format "
                + "FROM Showtime s JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.Status = ? AND s.StartTime > GETDATE() AND s.StartTime >= ? AND s.StartTime < ? "
                + "AND m.Status IN (?, ?) AND r.Status = ? ORDER BY s.StartTime, m.Title";
        List<Showtime> showtimes = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ShowtimeStatus.ACTIVE);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            ps.setString(4, MovieStatus.PUBLIC.get(0));
            ps.setString(5, MovieStatus.PUBLIC.get(1));
            ps.setString(6, RoomStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Showtime showtime = new Showtime();
                    showtime.setShowtimeId(rs.getInt("ShowtimeID"));
                    showtime.setMovieId(rs.getInt("MovieID"));
                    showtime.setRoomId(rs.getInt("RoomID"));
                    showtime.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
                    Timestamp end = rs.getTimestamp("EndTime");
                    showtime.setEndTime(end == null ? null : end.toLocalDateTime());
                    showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
                    showtime.setStatus(rs.getString("Status"));
                    showtime.setTranslationType(rs.getString("TranslationType"));
                    Movie movie = new Movie();
                    movie.setMovieId(rs.getInt("MovieID"));
                    movie.setTitle(rs.getString("Title"));
                    movie.setAgeRating(rs.getString("AgeRating"));
                    showtime.setMovie(movie);
                    Room room = new Room();
                    room.setRoomId(rs.getInt("RoomID"));
                    room.setRoomName(rs.getString("RoomName"));
                    room.setFormat(rs.getString("Format"));
                    showtime.setRoom(room);
                    showtimes.add(showtime);
                }
            }
        }
        return showtimes;
    }

    // Đơn tại quầy đã thu tiền ngay nên lưu thẳng trạng thái Paid, kèm nhân viên và ca; khách vãng lai CustomerID NULL
    public int insertBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO Booking (CustomerID, StaffID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, "
                + "FinalAmount, PaymentMethod, BookingTime, Status, Channel, PointsRedeemed, PointsEarned, ShiftID) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, booking.getCustomerId());
            ps.setInt(2, booking.getStaffId());
            ps.setInt(3, booking.getShowtimeId());
            setNullableInt(ps, 4, booking.getVoucherId());
            ps.setBigDecimal(5, booking.getTotalPrice());
            ps.setBigDecimal(6, booking.getDiscountAmount());
            ps.setBigDecimal(7, booking.getFinalAmount());
            ps.setString(8, booking.getPaymentMethod());
            ps.setString(9, booking.getStatus());
            ps.setString(10, booking.getChannel());
            ps.setInt(11, booking.getPointsRedeemed());
            ps.setInt(12, booking.getPointsEarned());
            ps.setInt(13, booking.getShiftId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được BookingID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    // Chỉ đơn bán tại quầy; null nếu không có
    public CounterReceipt findReceipt(int bookingId) throws SQLException {
        String sql = "SELECT b.BookingID, b.BookingTime, b.PaymentMethod, b.TotalPrice, b.DiscountAmount, "
                + "b.FinalAmount, b.PointsRedeemed, b.PointsEarned, m.Title, r.RoomName, s.StartTime, "
                + "st.FullName AS StaffName, c.FullName AS CustomerName, v.VoucherCode, sh.CounterNo "
                + "FROM Booking b JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
                + "LEFT JOIN [User] st ON st.UserID = b.StaffID LEFT JOIN [User] c ON c.UserID = b.CustomerID "
                + "LEFT JOIN Voucher v ON v.VoucherID = b.VoucherID LEFT JOIN Shift sh ON sh.ShiftID = b.ShiftID "
                + "WHERE b.BookingID = ? AND b.Channel = ?";
        try (Connection conn = DBContext.getConnection()) {
            CounterReceipt receipt;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookingId);
                ps.setString(2, BookingChannel.COUNTER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    receipt = new CounterReceipt();
                    receipt.setBookingId(rs.getInt("BookingID"));
                    receipt.setBookingTime(rs.getTimestamp("BookingTime").toLocalDateTime());
                    receipt.setPaymentMethod(rs.getString("PaymentMethod"));
                    receipt.setTotalPrice(rs.getBigDecimal("TotalPrice"));
                    receipt.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                    receipt.setFinalAmount(rs.getBigDecimal("FinalAmount"));
                    receipt.setPointsRedeemed(rs.getInt("PointsRedeemed"));
                    receipt.setPointsEarned(rs.getInt("PointsEarned"));
                    receipt.setMovieTitle(rs.getString("Title"));
                    receipt.setRoomName(rs.getString("RoomName"));
                    receipt.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
                    receipt.setStaffName(rs.getString("StaffName"));
                    receipt.setCustomerName(rs.getString("CustomerName"));
                    receipt.setVoucherCode(rs.getString("VoucherCode"));
                    receipt.setCounterNo(rs.getString("CounterNo"));
                }
            }
            String ticketSql = "SELECT se.RowName, se.ColNumber, t.TicketCode, t.Price FROM Ticket t "
                    + "JOIN Seat se ON se.SeatID = t.SeatID WHERE t.BookingID = ? ORDER BY se.RowName, se.ColNumber";
            try (PreparedStatement ps = conn.prepareStatement(ticketSql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        receipt.getTickets().add(new CounterReceipt.Line(
                                rs.getString("RowName") + rs.getInt("ColNumber"), rs.getString("TicketCode"), 1,
                                rs.getBigDecimal("Price")));
                    }
                }
            }
            String itemSql = "SELECT c.Name, bc.Quantity, bc.SubtotalPrice FROM Booking_Concession bc "
                    + "JOIN Concession c ON c.ConcessionID = bc.ConcessionID WHERE bc.BookingID = ? ORDER BY c.Name";
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                ps.setInt(1, bookingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        receipt.getConcessions().add(new CounterReceipt.Line(rs.getString("Name"), null,
                                rs.getInt("Quantity"), rs.getBigDecimal("SubtotalPrice")));
                    }
                }
            }
            return receipt;
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
