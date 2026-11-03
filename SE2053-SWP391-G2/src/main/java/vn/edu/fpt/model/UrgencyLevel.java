package vn.edu.fpt.model;

import java.util.List;

public final class UrgencyLevel {
    public static final String LOW = "Low";
    public static final String MEDIUM = "Medium";
    public static final String HIGH = "High";
    public static final String CRITICAL = "Critical";

    public static final List<String> ALL = List.of(LOW, MEDIUM, HIGH, CRITICAL);

    private UrgencyLevel() {
    }

    public static boolean isValid(String level) {
        return ALL.contains(level);
    }
}
