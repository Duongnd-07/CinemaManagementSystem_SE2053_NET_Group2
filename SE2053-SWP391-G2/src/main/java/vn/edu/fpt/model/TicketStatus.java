package vn.edu.fpt.model;

import java.util.List;

public final class TicketStatus {
    public static final String UPCOMING = "Upcoming";
    public static final String WATCHED = "Watched";
    public static final String EXCHANGED = "Exchanged";
    public static final String CANCELLED = "Cancelled";

    public static final List<String> ALL = List.of(UPCOMING, WATCHED, EXCHANGED, CANCELLED);

    private TicketStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
