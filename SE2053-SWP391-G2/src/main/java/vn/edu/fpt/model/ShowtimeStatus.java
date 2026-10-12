package vn.edu.fpt.model;

import java.util.List;

public final class ShowtimeStatus {
    public static final String ACTIVE = "Active";
    public static final String CANCELLED = "Cancelled";
    public static final String ENDED = "Ended";

    public static final List<String> ALL = List.of(ACTIVE, CANCELLED, ENDED);

    private ShowtimeStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
