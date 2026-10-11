package vn.edu.fpt.model;

import java.util.List;

public final class BookingStatus {
    public static final String PENDING = "Pending";
    public static final String PAID = "Paid";
    public static final String CANCELLED = "Cancelled";

    public static final List<String> ALL = List.of(PENDING, PAID, CANCELLED);

    private BookingStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
