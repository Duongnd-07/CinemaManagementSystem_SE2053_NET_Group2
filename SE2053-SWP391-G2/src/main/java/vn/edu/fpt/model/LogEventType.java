package vn.edu.fpt.model;

import java.util.List;
import java.util.Map;

// UC-32 BR-02: nhóm sự kiện được ghi vào nhật ký hệ thống
public final class LogEventType {
    public static final String AUTH = "AUTH";
    public static final String ACCOUNT = "ACCOUNT";
    public static final String CONFIG = "CONFIG";
    public static final String CATALOG = "CATALOG";
    public static final String BOOKING = "BOOKING";
    public static final String COUNTER = "COUNTER";
    public static final String SYSTEM = "SYSTEM";

    public static final List<String> ALL = List.of(AUTH, ACCOUNT, CONFIG, CATALOG, BOOKING, COUNTER, SYSTEM);

    private static final Map<String, String> LABELS = Map.of(
            AUTH, "Đăng nhập & xác thực",
            ACCOUNT, "Tài khoản",
            CONFIG, "Cấu hình hệ thống",
            CATALOG, "Quản lý dữ liệu",
            BOOKING, "Đặt vé & thanh toán",
            COUNTER, "Bán tại quầy",
            SYSTEM, "Lỗi hệ thống");

    private LogEventType() {
    }

    public static boolean isValid(String type) {
        return ALL.contains(type);
    }

    public static String labelOf(String type) {
        return LABELS.getOrDefault(type, type);
    }
}
