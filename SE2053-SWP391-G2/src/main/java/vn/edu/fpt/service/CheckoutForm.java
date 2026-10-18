package vn.edu.fpt.service;

import java.util.LinkedHashMap;
import java.util.Map;

// UC-13 bước 6-8: dữ liệu khách nhập ở trang checkout, chưa kiểm tra
public class CheckoutForm {
    // ConcessionID -> số lượng (chuỗi gốc)
    private Map<Integer, String> quantities = new LinkedHashMap<>();
    private String voucherCode;
    private String points;

    public CheckoutForm() {
    }

    public Map<Integer, String> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<Integer, String> quantities) {
        this.quantities = quantities;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
}
