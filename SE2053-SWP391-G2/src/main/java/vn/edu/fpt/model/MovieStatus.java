package vn.edu.fpt.model;

import java.util.List;

public final class MovieStatus {
    public static final String COMING_SOON = "Coming Soon";
    public static final String NOW_SHOWING = "Now Showing";
    public static final String ENDED = "Ended";
    public static final String HIDDEN = "Hidden";

    public static final List<String> ALL = List.of(COMING_SOON, NOW_SHOWING, ENDED, HIDDEN);

    private MovieStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
