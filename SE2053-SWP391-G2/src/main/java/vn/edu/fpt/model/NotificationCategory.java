package vn.edu.fpt.model;

import java.util.List;
import java.util.Map;

public final class NotificationCategory {
    public static final String GENERAL = "General";
    public static final String MOVIE = "Movie";
    public static final String INTERNAL = "Internal";
    public static final String SYSTEM = "System";
    public static final List<String> VALUES = List.of(GENERAL, MOVIE, INTERNAL, SYSTEM);
    private static final Map<String, String> LABELS = Map.of(
            GENERAL, "Thông báo chung", MOVIE, "Phim & lịch chiếu", INTERNAL, "Nội bộ",
            SYSTEM, "Hệ thống / bảo trì", "Review", "Đánh giá phim",
            "Refund", "Hoàn vé", "Voucher", "Voucher");

    private NotificationCategory() {
    }

    public static String labelOf(String type) {
        return type == null ? "" : LABELS.getOrDefault(type, type);
    }

    public static List<String> selectableFor(String target) {
        if (NotificationTarget.STAFF.equals(target)) return List.of(GENERAL, INTERNAL, SYSTEM);
        if (NotificationTarget.CUSTOMER.equals(target) || NotificationTarget.SINGLE_CUSTOMER.equals(target)) {
            return List.of(GENERAL, MOVIE, SYSTEM);
        }
        return NotificationTarget.ALL.equals(target) ? List.of(GENERAL, SYSTEM) : List.of();
    }

    public static boolean isAllowed(String type, String target) {
        return type != null && selectableFor(target).contains(type);
    }
}
