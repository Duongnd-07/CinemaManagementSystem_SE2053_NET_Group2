package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.DBContext;

public class ShowtimeDAO {

    private static final String SELECT_DETAILS = "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.StartTime, s.EndTime, "
            + "s.BasePrice, s.Status, r.RoomName, r.Format, r.TotalSeats, r.Status AS RoomStatus, "
            + "m.Title AS MovieTitle, m.DurationMinutes, m.AgeRating, m.PosterUrl, m.Formats AS MovieFormats, "
            + "m.ReleaseDate, m.Status AS MovieStatus, "
            + "(SELECT COUNT(*) FROM Ticket t WHERE t.ShowtimeID = s.ShowtimeID AND t.Status <> 'Cancelled') AS BookedTickets, "
            + "(SELECT COUNT(*) FROM Seat_Hold sh WHERE sh.ShowtimeID = s.ShowtimeID AND sh.ExpiryTime > GETDATE()) AS HeldSeats "
            + "FROM Showtime s "
            + "JOIN Room r ON r.RoomID = s.RoomID "
            + "JOIN Movie m ON m.MovieID = s.MovieID ";

    // UC-08: suất chiếu đang mở bán của một phim, bắt đầu trong khoảng (from, to), kèm phòng chiếu.
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

    // UC-24: Tự động chuyển các suất chiếu đã kết thúc sang trạng thái 'Ended'
    public int updateEndedShowtimes() throws SQLException {
        String sql = "UPDATE Showtime SET Status = ? WHERE (Status IS NULL OR Status = ?) AND EndTime <= GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ShowtimeStatus.ENDED);
            ps.setString(2, ShowtimeStatus.ACTIVE);
            return ps.executeUpdate();
        }
    }

    // UC-24: Chi tiết suất chiếu theo mã
    public Showtime findById(int showtimeId) throws SQLException {
        String sql = SELECT_DETAILS + "WHERE s.ShowtimeID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // UC-24: Danh sách suất chiếu theo bộ lọc tìm kiếm cho Manager (phân trang)
    public List<Showtime> search(Integer movieId, Integer roomId, LocalDate date, String status, int offset, int limit)
            throws SQLException {
        updateEndedShowtimes();
        String sql = SELECT_DETAILS + buildWhere(movieId, roomId, date, status)
                + "ORDER BY s.StartTime DESC, s.ShowtimeID DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilter(ps, movieId, roomId, date, status);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // UC-24: Tổng số suất chiếu thỏa bộ lọc
    public int count(Integer movieId, Integer roomId, LocalDate date, String status) throws SQLException {
        updateEndedShowtimes();
        String sql = "SELECT COUNT(*) FROM Showtime s "
                + "JOIN Room r ON r.RoomID = s.RoomID "
                + "JOIN Movie m ON m.MovieID = s.MovieID "
                + buildWhere(movieId, roomId, date, status);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, movieId, roomId, date, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // UC-24: Số lượng suất chiếu theo từng trạng thái (để hiển thị tab filter)
    public Map<String, Integer> countByStatus(Integer movieId, Integer roomId, LocalDate date) throws SQLException {
        updateEndedShowtimes();
        String sql = "SELECT s.Status, COUNT(*) AS Total FROM Showtime s "
                + "JOIN Room r ON r.RoomID = s.RoomID "
                + "JOIN Movie m ON m.MovieID = s.MovieID "
                + buildWhere(movieId, roomId, date, null)
                + "GROUP BY s.Status";
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, movieId, roomId, date, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return result;
    }

    /**
     * UC-24 / GB-08: Thuật toán chống trùng phòng chiếu (trên connection có sẵn để hỗ trợ Transaction)
     * Hai suất chiếu trong cùng phòng R chồng lấn nhau nếu:
     * existing.Status <> 'Cancelled' AND existing.StartTime < newEndTime AND existing.EndTime > newStartTime
     */
    public List<Showtime> findCollisions(Connection conn, int roomId, LocalDateTime startTime, LocalDateTime endTime,
            Integer excludeShowtimeId) throws SQLException {
        String sql = SELECT_DETAILS
                + "WHERE s.RoomID = ? "
                + "AND (s.Status IS NULL OR s.Status <> ?) "
                + "AND s.StartTime < ? AND s.EndTime > ? "
                + (excludeShowtimeId != null && excludeShowtimeId > 0 ? "AND s.ShowtimeID <> ? " : "")
                + "ORDER BY s.StartTime";
        List<Showtime> collisions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, roomId);
            ps.setString(idx++, ShowtimeStatus.CANCELLED);
            ps.setTimestamp(idx++, Timestamp.valueOf(endTime));
            ps.setTimestamp(idx++, Timestamp.valueOf(startTime));
            if (excludeShowtimeId != null && excludeShowtimeId > 0) {
                ps.setInt(idx++, excludeShowtimeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    collisions.add(mapRow(rs));
                }
            }
        }
        return collisions;
    }

    public List<Showtime> findCollisions(int roomId, LocalDateTime startTime, LocalDateTime endTime,
            Integer excludeShowtimeId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findCollisions(conn, roomId, startTime, endTime, excludeShowtimeId);
        }
    }

    // UC-24: Kiểm tra suất chiếu đã bán vé nào chưa (nếu đã bán vé thì CHẶN HỦY để bảo vệ khách)
    public boolean hasSoldTickets(int showtimeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // UC-24: Kiểm tra suất chiếu đã phát sinh vé đặt hoặc ghế đang giữ chỗ chưa (để khóa form sửa phim/phòng/giờ)
    public boolean hasBookings(int showtimeId) throws SQLException {
        String sql = "SELECT ( "
                + "  (SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ? AND Status <> 'Cancelled') + "
                + "  (SELECT COUNT(*) FROM Seat_Hold WHERE ShowtimeID = ? AND ExpiryTime > GETDATE()) "
                + ") AS Total";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }
        }
    }

    // UC-24: Kiểm tra xem có bất kỳ bản ghi nào trong DB tham chiếu tới ShowtimeID không (Ticket, Booking, Seat_Hold)
    // Nếu có dù chỉ 1 dòng tham chiếu thì CHẶN XÓA để tránh lỗi Foreign Key Constraint 500
    public boolean hasAnyReferences(int showtimeId) throws SQLException {
        String sql = "SELECT ( "
                + "  (SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ?) + "
                + "  (SELECT COUNT(*) FROM Booking WHERE ShowtimeID = ?) + "
                + "  (SELECT COUNT(*) FROM Seat_Hold WHERE ShowtimeID = ?) "
                + ") AS Total";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, showtimeId);
            ps.setInt(3, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }
        }
    }

    // UC-24: Thêm mới 1 suất chiếu
    public int insert(Connection conn, Showtime showtime) throws SQLException {
        String sql = "INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, showtime.getMovieId());
            ps.setInt(2, showtime.getRoomId());
            ps.setTimestamp(3, Timestamp.valueOf(showtime.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(showtime.getEndTime()));
            ps.setBigDecimal(5, showtime.getBasePrice());
            ps.setString(6, ShowtimeStatus.ACTIVE);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    // UC-24: Thêm mới danh sách suất chiếu hàng loạt trong 1 transaction
    public int[] insertBatch(Connection conn, List<Showtime> showtimes) throws SQLException {
        String sql = "INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Showtime s : showtimes) {
                ps.setInt(1, s.getMovieId());
                ps.setInt(2, s.getRoomId());
                ps.setTimestamp(3, Timestamp.valueOf(s.getStartTime()));
                ps.setTimestamp(4, Timestamp.valueOf(s.getEndTime()));
                ps.setBigDecimal(5, s.getBasePrice());
                ps.setString(6, ShowtimeStatus.ACTIVE);
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    // UC-24: Cập nhật thông tin suất chiếu
    public boolean update(Connection conn, Showtime showtime) throws SQLException {
        String sql = "UPDATE Showtime SET MovieID = ?, RoomID = ?, StartTime = ?, EndTime = ?, "
                + "BasePrice = ? WHERE ShowtimeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtime.getMovieId());
            ps.setInt(2, showtime.getRoomId());
            ps.setTimestamp(3, Timestamp.valueOf(showtime.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(showtime.getEndTime()));
            ps.setBigDecimal(5, showtime.getBasePrice());
            ps.setInt(6, showtime.getShowtimeId());
            return ps.executeUpdate() > 0;
        }
    }

    // UC-24: Hủy suất chiếu (đồng thời dọn sạch phiên giữ chỗ nếu có)
    public boolean cancel(int showtimeId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psHold = conn.prepareStatement("DELETE FROM Seat_Hold WHERE ShowtimeID = ?")) {
                    psHold.setInt(1, showtimeId);
                    psHold.executeUpdate();
                }
                boolean updated;
                try (PreparedStatement ps = conn.prepareStatement("UPDATE Showtime SET Status = ? WHERE ShowtimeID = ?")) {
                    ps.setString(1, ShowtimeStatus.CANCELLED);
                    ps.setInt(2, showtimeId);
                    updated = ps.executeUpdate() > 0;
                }
                conn.commit();
                return updated;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // UC-24: Xóa hẳn suất chiếu khỏi CSDL
    public boolean delete(int showtimeId) throws SQLException {
        String sql = "DELETE FROM Showtime WHERE ShowtimeID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            return ps.executeUpdate() > 0;
        }
    }

    private String buildWhere(Integer movieId, Integer roomId, LocalDate date, String status) {
        List<String> conditions = new ArrayList<>();
        if (movieId != null && movieId > 0) {
            conditions.add("s.MovieID = ?");
        }
        if (roomId != null && roomId > 0) {
            conditions.add("s.RoomID = ?");
        }
        if (date != null) {
            conditions.add("CAST(s.StartTime AS DATE) = ?");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("s.Status = ?");
        }
        return conditions.isEmpty() ? " " : " WHERE " + String.join(" AND ", conditions) + " ";
    }

    private int bindFilter(PreparedStatement ps, Integer movieId, Integer roomId, LocalDate date, String status)
            throws SQLException {
        int idx = 1;
        if (movieId != null && movieId > 0) {
            ps.setInt(idx++, movieId);
        }
        if (roomId != null && roomId > 0) {
            ps.setInt(idx++, roomId);
        }
        if (date != null) {
            ps.setDate(idx++, Date.valueOf(date));
        }
        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }
        return idx;
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

        try {
            String title = rs.getString("MovieTitle");
            if (title != null) {
                Movie movie = new Movie();
                movie.setMovieId(rs.getInt("MovieID"));
                movie.setTitle(title);
                movie.setDurationMinutes(rs.getInt("DurationMinutes"));
                movie.setAgeRating(rs.getString("AgeRating"));
                movie.setPosterUrl(rs.getString("PosterUrl"));
                movie.setFormats(rs.getString("MovieFormats"));
                Date relDate = rs.getDate("ReleaseDate");
                if (relDate != null) {
                    movie.setReleaseDate(relDate.toLocalDate());
                }
                movie.setStatus(rs.getString("MovieStatus"));
                showtime.setMovie(movie);
            }
        } catch (SQLException ignored) {
        }

        try {
            showtime.setBookedTickets(rs.getInt("BookedTickets"));
        } catch (SQLException ignored) {
        }

        return showtime;
    }
}
