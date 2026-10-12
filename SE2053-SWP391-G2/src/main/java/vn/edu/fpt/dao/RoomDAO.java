package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.util.DBContext;

public class RoomDAO {

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT RoomID, RoomName, Format, TotalSeats, Status FROM Room ORDER BY RoomName";
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Room room = new Room();
                room.setRoomId(rs.getInt("RoomID"));
                room.setRoomName(rs.getString("RoomName"));
                room.setFormat(rs.getString("Format"));
                room.setTotalSeats(rs.getInt("TotalSeats"));
                room.setStatus(rs.getString("Status"));
                rooms.add(room);
            }
        }
        return rooms;
    }
}
