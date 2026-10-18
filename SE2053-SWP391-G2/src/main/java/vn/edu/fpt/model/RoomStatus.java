package vn.edu.fpt.model;

import java.util.List;

// Tình trạng vật lý, dùng chung cho cột Status của Room và Seat
public final class RoomStatus {
    public static final String ACTIVE = "Active";
    public static final String MAINTENANCE = "Maintenance";
    // Chỉ dùng cho Room: phòng dừng hoạt động do Manager đặt (UC-25); ghế không có trạng thái này nên không nằm trong ALL
    public static final String INACTIVE = "Inactive";

    public static final List<String> ALL = List.of(ACTIVE, MAINTENANCE);

    private RoomStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
