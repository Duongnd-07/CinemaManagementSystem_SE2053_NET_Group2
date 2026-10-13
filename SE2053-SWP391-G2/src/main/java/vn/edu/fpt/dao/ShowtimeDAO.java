package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.DBContext;

public class ShowtimeDAO {

    // UC-08: suất chiếu đang mở bán của một phim, bắt đầu trong khoảng (from, to), kèm phòng chiếu.
    // Chỉ lấy khi phim đang ở một trong các trạng thái movieStatuses (Đang chiếu / Sắp chiếu).
    public List<Showtime> findActiveByMovie(int movieId, LocalDateTime from, LocalDateTime to,
            List<String> movieStatuses) throws SQLException {
        List<Showtime> showtimes = new ArrayList<>();
        if (movieStatuses.isEmpty()) {
            return showtimes;
        }
        String sql = "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.StartTime, s.EndTime, s.BasePrice, s.Status, "
                + "r.RoomName, r.Format, r.TotalSeats, r.Status AS RoomStatus "
                + "FROM Showtime s "
                + "JOIN Room r ON r.RoomID = s.RoomID "
                + "JOIN Movie m ON m.MovieID = s.MovieID "
                + "WHERE s.MovieID = ? AND s.StartTime > ? AND s.StartTime < ? "
                + "AND (s.Status IS NULL OR s.Status = ?) "
                + "AND m.Status IN (" + String.join(",", Collections.nCopies(movieStatuses.size(), "?")) + ") "
                + "ORDER BY s.StartTime, r.RoomName";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, movieId);
            ps.setTimestamp(idx++, Timestamp.valueOf(from));
            ps.setTimestamp(idx++, Timestamp.valueOf(to));
            ps.setString(idx++, ShowtimeStatus.ACTIVE);
            for (String status : movieStatuses) {
                ps.setString(idx++, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    showtimes.add(mapRow(rs));
                }
            }
        }
        return showtimes;
    }

    private Showtime mapRow(ResultSet rs) throws SQLException {
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(rs.getInt("ShowtimeID"));
        showtime.setMovieId(rs.getInt("MovieID"));
        showtime.setRoomId(rs.getInt("RoomID"));
        showtime.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
        Timestamp end = rs.getTimestamp("EndTime");
        showtime.setEndTime(end == null ? null : end.toLocalDateTime());
        showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
        showtime.setStatus(rs.getString("Status"));
        Room room = new Room();
        room.setRoomId(rs.getInt("RoomID"));
        room.setRoomName(rs.getString("RoomName"));
        room.setFormat(rs.getString("Format"));
        room.setTotalSeats(rs.getInt("TotalSeats"));
        room.setStatus(rs.getString("RoomStatus"));
        showtime.setRoom(room);
        return showtime;
    }
}
