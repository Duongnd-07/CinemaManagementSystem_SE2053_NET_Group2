package vn.edu.fpt.model;

import java.util.List;

// Loại ghế, khớp ràng buộc CHECK của cột Seat.SeatType.
// Ghế đôi (Sweetbox) là MỘT bản ghi Seat chiếm 2 cột liền nhau (ColNumber là cột bên trái) và chứa 2 người.
public final class SeatType {
    public static final String STANDARD = "Standard";
    public static final String VIP = "VIP";
    // Ghế đôi (Couple)
    public static final String SWEETBOX = "Sweetbox";

    public static final List<String> ALL = List.of(STANDARD, VIP, SWEETBOX);

    private SeatType() {
    }

    public static boolean isValid(String type) {
        return ALL.contains(type);
    }

    // Số cột ghế chiếm trên sơ đồ
    public static int widthOf(String type) {
        return SWEETBOX.equals(type) ? 2 : 1;
    }

    // Số người một ghế chứa (sức chứa phòng, tỷ lệ lấp đầy)
    public static int capacityOf(String type) {
        return SWEETBOX.equals(type) ? 2 : 1;
    }
}
