package vn.edu.fpt.model;

import java.util.List;

public final class VoucherType {
    public static final String PROMOTION = "Promotion";
    public static final String REFUND_80 = "Refund80";
    public static final List<String> ALL = List.of(PROMOTION, REFUND_80);

    private VoucherType() {
    }
}
