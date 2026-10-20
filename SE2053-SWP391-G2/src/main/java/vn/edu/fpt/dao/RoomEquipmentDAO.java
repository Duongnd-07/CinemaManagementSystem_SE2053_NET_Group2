package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.RoomEquipment;
import vn.edu.fpt.util.DBContext;

// UC-21: thiết bị cụ thể của từng phòng (bảng RoomEquipment, script UC21_room_equipment.sql)
public class RoomEquipmentDAO {

    private static final String SELECT_COLUMNS = "SELECT EquipmentID, RoomID, Category, Name FROM RoomEquipment ";

    public List<RoomEquipment> findAll() throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_COLUMNS + "ORDER BY RoomID, Category, Name")) {
            return readAll(ps);
        }
    }

    public List<RoomEquipment> findByRoomAndCategory(int roomId, String category) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE RoomID = ? AND Category = ? ORDER BY Name";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, category);
            return readAll(ps);
        }
    }

    private List<RoomEquipment> readAll(PreparedStatement ps) throws SQLException {
        List<RoomEquipment> items = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RoomEquipment item = new RoomEquipment();
                item.setEquipmentId(rs.getInt("EquipmentID"));
                item.setRoomId(rs.getInt("RoomID"));
                item.setCategory(rs.getString("Category"));
                item.setName(rs.getString("Name"));
                items.add(item);
            }
        }
        return items;
    }
}
