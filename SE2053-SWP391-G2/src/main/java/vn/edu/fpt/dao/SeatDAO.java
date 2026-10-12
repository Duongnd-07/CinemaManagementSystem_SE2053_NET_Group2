package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;

public class SeatDAO {

    private static final String SELECT_COLUMNS = "SELECT SeatID, RoomID, RowName, ColNumber, SeatType, Status FROM Seat ";

    // Xếp theo phòng, hàng ngắn đứng trước (A..Z rồi AA..), trong hàng theo số ghế
    public List<Seat> findAll() throws SQLException {
        String sql = SELECT_COLUMNS + "ORDER BY RoomID, LEN(RowName), RowName, ColNumber";
        List<Seat> seats = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                seats.add(mapRow(rs));
            }
        }
        return seats;
    }

    public Seat findById(int seatId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE SeatID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public void updateStatus(Connection conn, int seatId, String status) throws SQLException {
        String sql = "UPDATE Seat SET Status = ? WHERE SeatID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, seatId);
            ps.executeUpdate();
        }
    }

    // Số vé còn hiệu lực của ghế cho các suất chưa kết thúc
    public int countUpcomingTickets(int seatId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ticket t "
                + "JOIN Booking b ON b.BookingID = t.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "WHERE t.SeatID = ? AND t.Status = ? AND b.Status = ? AND s.EndTime > GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seatId);
            ps.setString(2, TicketStatus.UPCOMING);
            ps.setString(3, BookingStatus.PAID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Seat mapRow(ResultSet rs) throws SQLException {
        Seat seat = new Seat();
        seat.setSeatId(rs.getInt("SeatID"));
        seat.setRoomId(rs.getInt("RoomID"));
        seat.setRowName(rs.getString("RowName"));
        seat.setColNumber(rs.getInt("ColNumber"));
        seat.setSeatType(rs.getString("SeatType"));
        seat.setStatus(rs.getString("Status"));
        return seat;
    }
}
