package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.User;

// UC-19: kết quả tính tiền ở quầy (bước 5 của SRS) và lỗi từng phần để nhân viên sửa trước khi thu tiền
public class CounterQuote {
    final List<BookingSeatLine> seats = new ArrayList<>();
    final List<BookingConcessionLine> concessions = new ArrayList<>();
    final List<CheckoutVoucherOption> vouchers = new ArrayList<>();
    final Map<String, String> errors = new LinkedHashMap<>();
    User customer;
    Integer voucherId;
    String voucherCode;
    BigDecimal ticketTotal = BigDecimal.ZERO;
    BigDecimal concessionTotal = BigDecimal.ZERO;
    BigDecimal voucherDiscount = BigDecimal.ZERO;
    int pointsRedeemed;
    int pointRate = 1;
    int availablePoints;
    int maxPoints;
    BigDecimal changeDue;
    // Mã đơn sau khi xác nhận bán thành công
    int bookingId;

    public List<BookingSeatLine> getSeats() {
        return seats;
    }

    public List<BookingConcessionLine> getConcessions() {
        return concessions;
    }

    public List<CheckoutVoucherOption> getVouchers() {
        return vouchers;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public User getCustomer() {
        return customer;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public BigDecimal getTicketTotal() {
        return ticketTotal;
    }

    public BigDecimal getConcessionTotal() {
        return concessionTotal;
    }

    public BigDecimal getGrossTotal() {
        return ticketTotal.add(concessionTotal);
    }

    public BigDecimal getVoucherDiscount() {
        return voucherDiscount;
    }

    public int getPointsRedeemed() {
        return pointsRedeemed;
    }

    public BigDecimal getPointsDiscount() {
        return BigDecimal.valueOf((long) pointsRedeemed * pointRate);
    }

    public BigDecimal getDiscountTotal() {
        return voucherDiscount.add(getPointsDiscount());
    }

    // Số tiền nhân viên cần thu của khách
    public BigDecimal getFinalAmount() {
        return getGrossTotal().subtract(getDiscountTotal()).max(BigDecimal.ZERO);
    }

    public int getPointRate() {
        return pointRate;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    // Tiền thối khi khách trả tiền mặt; null nếu chưa nhập tiền khách đưa
    public BigDecimal getChangeDue() {
        return changeDue;
    }

    public int getBookingId() {
        return bookingId;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
