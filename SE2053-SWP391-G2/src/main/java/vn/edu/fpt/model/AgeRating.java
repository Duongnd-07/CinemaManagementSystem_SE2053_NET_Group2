package vn.edu.fpt.model;

import java.util.List;

public final class AgeRating {
    public static final List<String> ALL = List.of("P", "C13", "C16", "C18");

    private AgeRating() {
    }

    public static boolean isValid(String rating) {
        return ALL.contains(rating);
    }
}
