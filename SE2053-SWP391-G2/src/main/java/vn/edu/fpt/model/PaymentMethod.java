package vn.edu.fpt.model;

import java.util.List;

public final class PaymentMethod {
    public static final String CASH = "Cash";
    public static final String CARD = "Card";
    public static final String TRANSFER = "Transfer";
    public static final String VNPAY = "VNPay";
    public static final String SEPAY = "SePay";

    public static final List<String> ALL = List.of(CASH, CARD, TRANSFER, VNPAY, SEPAY);
    // UC-19: quầy chỉ thu tiền mặt, quẹt thẻ hoặc chuyển khoản QR
    public static final List<String> COUNTER = List.of(CASH, CARD, TRANSFER);

    private PaymentMethod() {
    }

    public static boolean isValid(String method) {
        return ALL.contains(method);
    }
}
