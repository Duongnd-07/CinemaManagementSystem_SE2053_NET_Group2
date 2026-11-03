package vn.edu.fpt.model;

import java.util.List;

public final class ConcessionStatus {
    public static final String ACTIVE = "Active";
    public static final String OUT_OF_STOCK = "Out of Stock";
    public static final String HIDDEN = "Hidden";

    public static final List<String> ALL = List.of(ACTIVE, OUT_OF_STOCK, HIDDEN);

    private ConcessionStatus() {
    }

    public static boolean isValid(String status) {
        return ALL.contains(status);
    }
}
