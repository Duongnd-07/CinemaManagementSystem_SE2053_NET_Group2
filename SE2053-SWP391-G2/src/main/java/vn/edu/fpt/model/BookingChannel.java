package vn.edu.fpt.model;

import java.util.List;

public final class BookingChannel {
    public static final String ONLINE = "Online";
    public static final String COUNTER = "Counter";

    public static final List<String> ALL = List.of(ONLINE, COUNTER);

    private BookingChannel() {
    }

    public static boolean isValid(String channel) {
        return ALL.contains(channel);
    }
}
