package vn.edu.fpt.model;

import java.util.List;

// GB-02: trạng thái của một ghế trong một suất chiếu
public final class SeatStatus {
    public static final String AVAILABLE = "AVAILABLE";
    public static final String PENDING = "PENDING";
    public static final String SOLD = "SOLD";
    public static final String MAINTENANCE = "MAINTENANCE";

    public static final List<String> ALL = List.of(AVAILABLE, PENDING, SOLD, MAINTENANCE);

    private SeatStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
