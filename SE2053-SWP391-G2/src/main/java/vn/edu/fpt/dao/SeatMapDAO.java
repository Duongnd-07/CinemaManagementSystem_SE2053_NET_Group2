package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;

// UC-09: truy vấn chỉ đọc phục vụ sơ đồ ghế của một suất chiếu
public class SeatMapDAO {

    public Showtime findShowtime(int showtimeId) throws SQLException {
        String sql = "SELECT ShowtimeID, MovieID, RoomID, StartTime, EndTime, BasePrice, Status "
                + "FROM Showtime WHERE ShowtimeID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Showtime showtime = new Showtime();
                showtime.setShowtimeId(rs.getInt("ShowtimeID"));
                showtime.setMovieId(rs.getInt("MovieID"));
                showtime.setRoomId(rs.getInt("RoomID"));
                Timestamp start = rs.getTimestamp("StartTime");
                showtime.setStartTime(start == null ? null : start.toLocalDateTime());
                Timestamp end = rs.getTimestamp("EndTime");
                showtime.setEndTime(end == null ? null : end.toLocalDateTime());
                showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
                showtime.setStatus(rs.getString("Status"));
                return showtime;
            }
        }
    }

    public Room findRoom(int roomId) throws SQLException {
        String sql = "SELECT RoomID, RoomName, Format, TotalSeats, Status FROM Room WHERE RoomID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Room room = new Room();
                room.setRoomId(rs.getInt("RoomID"));
                room.setRoomName(rs.getString("RoomName"));
                room.setFormat(rs.getString("Format"));
                room.setTotalSeats(rs.getInt("TotalSeats"));
                room.setStatus(rs.getString("Status"));
                return room;
            }
        }
    }

    // Hàng ngắn đứng trước (A..Z rồi AA..), trong hàng xếp theo số ghế
    public List<Seat> findSeatsByRoom(int roomId) throws SQLException {
        String sql = "SELECT SeatID, RoomID, RowName, ColNumber, SeatType, Status FROM Seat "
                + "WHERE RoomID = ? ORDER BY LEN(RowName), RowName, ColNumber";
        List<Seat> seats = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat seat = new Seat();
                    seat.setSeatId(rs.getInt("SeatID"));
                    seat.setRoomId(rs.getInt("RoomID"));
                    seat.setRowName(rs.getString("RowName"));
                    seat.setColNumber(rs.getInt("ColNumber"));
                    seat.setSeatType(rs.getString("SeatType"));
                    seat.setStatus(rs.getString("Status"));
                    seats.add(seat);
                }
            }
        }
        return seats;
    }

    // Ghế có vé còn hiệu lực thuộc đơn đã thanh toán
    public Set<Integer> findSoldSeatIds(int showtimeId) throws SQLException {
        String sql = "SELECT t.SeatID FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "WHERE b.ShowtimeID = ? AND b.Status = ? AND t.Status IN (?, ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setString(2, BookingStatus.PAID);
            ps.setString(3, TicketStatus.UPCOMING);
            ps.setString(4, TicketStatus.WATCHED);
            return readSeatIds(ps);
        }
    }

    // GB-01: ghế đang giữ tạm (hold chưa hết hạn) hoặc đã gắn vào đơn chưa thanh toán
    public Set<Integer> findPendingSeatIds(int showtimeId) throws SQLException {
        String sql = "SELECT SeatID FROM Seat_Hold WHERE ShowtimeID = ? AND ExpiryTime > GETDATE() "
                + "UNION SELECT t.SeatID FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "WHERE b.ShowtimeID = ? AND b.Status = ? AND t.Status IN (?, ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, showtimeId);
            ps.setString(3, BookingStatus.PENDING);
            ps.setString(4, TicketStatus.UPCOMING);
            ps.setString(5, TicketStatus.WATCHED);
            return readSeatIds(ps);
        }
    }

    private Set<Integer> readSeatIds(PreparedStatement ps) throws SQLException {
        Set<Integer> seatIds = new HashSet<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                seatIds.add(rs.getInt("SeatID"));
            }
        }
        return seatIds;
    }
}
