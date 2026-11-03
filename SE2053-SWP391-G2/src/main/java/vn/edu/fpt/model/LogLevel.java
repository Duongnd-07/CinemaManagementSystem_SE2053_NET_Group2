package vn.edu.fpt.model;

import java.util.List;
import java.util.Map;

// UC-32 BR-03: mức độ của một dòng nhật ký hệ thống
public final class LogLevel {
    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    public static final List<String> ALL = List.of(INFO, WARNING, ERROR);

    private static final Map<String, String> LABELS = Map.of(
            INFO, "Thông tin",
            WARNING, "Cảnh báo",
            ERROR, "Lỗi");

    private LogLevel() {
    }

    public static boolean isValid(String level) {
        return ALL.contains(level);
    }

    public static String labelOf(String level) {
        return LABELS.getOrDefault(level, level);
    }
}
