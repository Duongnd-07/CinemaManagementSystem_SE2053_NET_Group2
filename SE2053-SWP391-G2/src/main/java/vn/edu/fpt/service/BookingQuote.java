package vn.edu.fpt.service;

import java.math.BigDecimal;

// UC-13 bước 8: bảng tính tiền của đơn.
// Tổng = tiền vé + bắp nước; voucher giảm trên tổng (GB-05); điểm chỉ trừ vào tiền vé (BR-LOYALTY-01)
public class BookingQuote {
    private final BigDecimal ticketTotal;
    private final BigDecimal concessionTotal;
    private final Integer voucherId;
    private final String voucherCode;
    private final BigDecimal voucherDiscount;
    private final int pointsRedeemed;
    private final BigDecimal pointsDiscount;
    private final int maxRedeemablePoints;

    public BookingQuote(BigDecimal ticketTotal, BigDecimal concessionTotal, Integer voucherId, String voucherCode,
                        BigDecimal voucherDiscount, int pointsRedeemed, BigDecimal pointsDiscount,
                        int maxRedeemablePoints) {
        this.ticketTotal = ticketTotal;
        this.concessionTotal = concessionTotal;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.voucherDiscount = voucherDiscount;
        this.pointsRedeemed = pointsRedeemed;
        this.pointsDiscount = pointsDiscount;
        this.maxRedeemablePoints = maxRedeemablePoints;
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

    public Integer getVoucherId() {
        return voucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public BigDecimal getVoucherDiscount() {
        return voucherDiscount;
    }

    public int getPointsRedeemed() {
        return pointsRedeemed;
    }

    public BigDecimal getPointsDiscount() {
        return pointsDiscount;
    }

    public BigDecimal getDiscountTotal() {
        return voucherDiscount.add(pointsDiscount);
    }

    public BigDecimal getFinalAmount() {
        return getGrossTotal().subtract(getDiscountTotal());
    }

    public int getMaxRedeemablePoints() {
        return maxRedeemablePoints;
    }
}
