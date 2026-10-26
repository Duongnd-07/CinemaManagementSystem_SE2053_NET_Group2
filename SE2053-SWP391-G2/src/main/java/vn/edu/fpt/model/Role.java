package vn.edu.fpt.model;

import java.util.List;

public final class Role {
    public static final String ADMIN = "Admin";
    public static final String MANAGER = "Manager";
    public static final String STAFF = "Staff";
    public static final String CUSTOMER = "Customer";

    public static final List<String> ALL = List.of(ADMIN, MANAGER, STAFF, CUSTOMER);

    private Role() {
    }

    public static boolean isValid(String role) {
        return ALL.contains(role);
    }
}
