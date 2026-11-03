package vn.edu.fpt.model;

import java.util.List;
import java.util.Map;

public final class NotificationTarget {
    public static final String ALL = "All";
    public static final String STAFF = "Staff";
    public static final String CUSTOMER = "Customer";
    public static final String SINGLE_CUSTOMER = "SingleCustomer";
    public static final List<String> VALUES = List.of(ALL, STAFF, CUSTOMER, SINGLE_CUSTOMER);
    private static final Map<String, String> LABELS = Map.of(
            ALL, "Toàn bộ tài khoản", STAFF, "Toàn bộ nhân viên",
            CUSTOMER, "Toàn bộ khách hàng", SINGLE_CUSTOMER, "Một khách hàng");

    private NotificationTarget() {
    }

    public static boolean isValid(String target) {
        return target != null && VALUES.contains(target);
    }

    public static String labelOf(String target) {
        return target == null ? "" : LABELS.getOrDefault(target, target);
    }

    public static List<String> recipientRoles(String target) {
        if (ALL.equals(target)) return Role.ALL;
        if (STAFF.equals(target)) return List.of(Role.STAFF, Role.MANAGER);
        if (CUSTOMER.equals(target) || SINGLE_CUSTOMER.equals(target)) return List.of(Role.CUSTOMER);
        return List.of();
    }
}
