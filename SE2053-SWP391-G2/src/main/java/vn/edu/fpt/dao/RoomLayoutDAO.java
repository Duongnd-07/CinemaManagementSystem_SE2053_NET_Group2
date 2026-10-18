package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.RoomSummary;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;

// UC-25: đọc/ghi phòng chiếu cùng toàn bộ sơ đồ ghế
public class RoomLayoutDAO {

    // Danh sách phòng kèm số ghế dùng được / đang khóa và số suất chiếu chưa kết thúc
    public List<RoomSummary> findAllSummaries() throws SQLException {
        String sql = "SELECT r.RoomID, r.RoomName, r.Format, r.TotalSeats, r.Status, "
                + "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID AND s.Status = ?) AS ActiveSeats, "
                + "(SELECT COUNT(*) FROM Seat s WHERE s.RoomID = r.RoomID AND s.Status <> ?) AS DisabledSeats, "
                + "(SELECT COUNT(*) FROM Showtime sh WHERE sh.RoomID = r.RoomID AND sh.Status = ? "
                + "AND sh.EndTime > GETDATE()) AS Upcoming "
                + "FROM Room r ORDER BY r.RoomName";
        List<RoomSummary> rooms = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, RoomStatus.ACTIVE);
            ps.setString(2, RoomStatus.ACTIVE);
            ps.setString(3, ShowtimeStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomSummary summary = new RoomSummary();
                    summary.setRoom(mapRoom(rs));
                    summary.setActiveSeats(rs.getInt("ActiveSeats"));
                    summary.setDisabledSeats(rs.getInt("DisabledSeats"));
                    summary.setUpcomingShowtimes(rs.getInt("Upcoming"));
                    rooms.add(summary);
                }
            }
        }
        return rooms;
    }

    public Room findRoom(int roomId) throws SQLException {
        String sql = "SELECT RoomID, RoomName, Format, TotalSeats, Status FROM Room WHERE RoomID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRoom(rs) : null;
            }
        }
    }

    // So sánh theo collation của cột (CI) để khớp đúng ràng buộc UNIQUE(RoomName)
    public boolean existsByName(String roomName, int excludeRoomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Room WHERE RoomName = ? AND RoomID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomName);
            ps.setInt(2, excludeRoomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<Seat> findSeats(int roomId) throws SQLException {
        String sql = "SELECT SeatID, RoomID, RowName, ColNumber, SeatType, Status FROM Seat "
                + "WHERE RoomID = ? ORDER BY RowName, ColNumber";
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

    // Số suất chiếu đang mở bán chưa kết thúc của phòng
    public int countUpcomingShowtimes(int roomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Showtime WHERE RoomID = ? AND Status = ? AND EndTime > GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, ShowtimeStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Ghế đã được tham chiếu (vé, giữ chỗ, báo cáo sự cố) nên không xóa được khỏi sơ đồ (khóa ngoại)
    public Set<Integer> findSeatIdsWithHistory(int roomId) throws SQLException {
        String sql = "SELECT s.SeatID FROM Seat s WHERE s.RoomID = ? AND ("
                + "EXISTS (SELECT 1 FROM Ticket t WHERE t.SeatID = s.SeatID) "
                + "OR EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.SeatID = s.SeatID) "
                + "OR EXISTS (SELECT 1 FROM Equipment_Report e WHERE e.SeatID = s.SeatID))";
        return findSeatIds(sql, roomId);
    }

    // Ghế đã bán vé (còn hiệu lực) cho suất chiếu chưa kết thúc - cùng tiêu chí với SeatDAO.countUpcomingTickets
    public Set<Integer> findSeatIdsWithUpcomingTickets(int roomId) throws SQLException {
        String sql = "SELECT DISTINCT t.SeatID FROM Ticket t "
                + "JOIN Booking b ON b.BookingID = t.BookingID "
                + "JOIN Showtime sh ON sh.ShowtimeID = b.ShowtimeID "
                + "JOIN Seat s ON s.SeatID = t.SeatID "
                + "WHERE s.RoomID = ? AND t.Status = ? AND b.Status = ? AND sh.EndTime > GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, TicketStatus.UPCOMING);
            ps.setString(3, BookingStatus.PAID);
            return readIds(ps);
        }
    }

    // Ghế đang có báo cáo sự cố chưa xử lý xong (UC-21/26 khóa ghế cho tới khi Resolved)
    public Set<Integer> findSeatIdsWithOpenReports(int roomId) throws SQLException {
        String sql = "SELECT DISTINCT e.SeatID FROM Equipment_Report e JOIN Seat s ON s.SeatID = e.SeatID "
                + "WHERE s.RoomID = ? AND e.Status <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, ReportStatus.RESOLVED);
            return readIds(ps);
        }
    }

    public int insertRoom(Connection conn, Room room) throws SQLException {
        String sql = "INSERT INTO Room (RoomName, Format, TotalSeats, Status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomName());
            ps.setString(2, room.getFormat());
            ps.setInt(3, room.getTotalSeats());
            ps.setString(4, room.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được RoomID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    // Trạng thái phòng (Active/Maintenance) do UC-26 quản lý nên không cập nhật ở đây
    public void updateRoom(Connection conn, Room room) throws SQLException {
        String sql = "UPDATE Room SET RoomName = ?, Format = ?, TotalSeats = ? WHERE RoomID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomName());
            ps.setString(2, room.getFormat());
            ps.setInt(3, room.getTotalSeats());
            ps.setInt(4, room.getRoomId());
            ps.executeUpdate();
        }
    }

    // Sửa thông tin phòng (tên, định dạng, trạng thái). Chỉ ghi khi Status trong DB vẫn là expectedStatus,
    // để không ghi đè trạng thái Bảo trì mà UC-26 vừa đặt trong lúc Manager đang sửa; trả về false nếu đã đổi
    public boolean updateRoomInfo(Room room, String expectedStatus) throws SQLException {
        String sql = "UPDATE Room SET RoomName = ?, Format = ?, Status = ? WHERE RoomID = ? AND Status = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room.getRoomName());
            ps.setString(2, room.getFormat());
            ps.setString(3, room.getStatus());
            ps.setInt(4, room.getRoomId());
            ps.setString(5, expectedStatus);
            return ps.executeUpdate() > 0;
        }
    }

    public void insertSeats(Connection conn, int roomId, List<Seat> seats) throws SQLException {
        if (seats.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO Seat (RoomID, RowName, ColNumber, SeatType, Status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Seat seat : seats) {
                ps.setInt(1, roomId);
                ps.setString(2, seat.getRowName());
                ps.setInt(3, seat.getColNumber());
                ps.setString(4, seat.getSeatType());
                ps.setString(5, seat.getStatus());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void updateSeats(Connection conn, List<Seat> seats) throws SQLException {
        if (seats.isEmpty()) {
            return;
        }
        String sql = "UPDATE Seat SET SeatType = ?, Status = ? WHERE SeatID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Seat seat : seats) {
                ps.setString(1, seat.getSeatType());
                ps.setString(2, seat.getStatus());
                ps.setInt(3, seat.getSeatId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void deleteSeats(Connection conn, List<Integer> seatIds) throws SQLException {
        if (seatIds.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Seat WHERE SeatID = ?")) {
            for (int seatId : seatIds) {
                ps.setInt(1, seatId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private Set<Integer> findSeatIds(String sql, int roomId) throws SQLException {
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            return readIds(ps);
        }
    }

    private Set<Integer> readIds(PreparedStatement ps) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        Room room = new Room();
        room.setRoomId(rs.getInt("RoomID"));
        room.setRoomName(rs.getString("RoomName"));
        room.setFormat(rs.getString("Format"));
        room.setTotalSeats(rs.getInt("TotalSeats"));
        room.setStatus(rs.getString("Status"));
        return room;
    }
}
