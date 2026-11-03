package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.ReportStatus;
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

    // Loại thiết bị của phòng có hơn một nửa số thiết bị đang có báo cáo chưa xử lý xong (UC-21 tự khóa phòng).
    // Giá trị: {số thiết bị hỏng, tổng số thiết bị của loại đó trong phòng}
    public Map<String, int[]> findOverHalfBroken(Connection conn, int roomId) throws SQLException {
        String sql = "SELECT e.Category, COUNT(*) AS Total, COUNT(r.EquipmentID) AS Broken FROM RoomEquipment e "
                + "OUTER APPLY (SELECT TOP 1 EquipmentID FROM Equipment_Report "
                + "WHERE EquipmentID = e.EquipmentID AND Status <> ?) r "
                + "WHERE e.RoomID = ? GROUP BY e.Category ORDER BY e.Category";
        Map<String, int[]> overHalf = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ReportStatus.RESOLVED);
            ps.setInt(2, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("Total");
                    int broken = rs.getInt("Broken");
                    if (broken * 2 > total) {
                        overHalf.put(rs.getString("Category"), new int[]{broken, total});
                    }
                }
            }
        }
        return overHalf;
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
