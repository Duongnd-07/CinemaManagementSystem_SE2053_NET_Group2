package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// UC-13 bước 7: một voucher trong ví của khách, kèm số tiền giảm ước tính cho đơn hiện tại và lý do chưa dùng được
public class CheckoutVoucherOption {
    private final String voucherCode;
    private final String discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minOrderAmount;
    private final LocalDateTime expiryDate;
    private final boolean refundVoucher;
    private final BigDecimal estimatedDiscount;
    // GB-05: đơn chưa đạt giá trị tối thiểu
    private final boolean belowMinOrder;
    // Voucher đã gắn vào một đơn khác đang chờ thanh toán
    private final boolean inOtherOrder;
    private final boolean applied;

    public CheckoutVoucherOption(String voucherCode, String discountType, BigDecimal discountValue,
                                 BigDecimal minOrderAmount, LocalDateTime expiryDate, boolean refundVoucher,
                                 BigDecimal estimatedDiscount, boolean belowMinOrder, boolean inOtherOrder,
                                 boolean applied) {
        this.voucherCode = voucherCode;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.expiryDate = expiryDate;
        this.refundVoucher = refundVoucher;
        this.estimatedDiscount = estimatedDiscount;
        this.belowMinOrder = belowMinOrder;
        this.inOtherOrder = inOtherOrder;
        this.applied = applied;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public String getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public boolean isRefundVoucher() {
        return refundVoucher;
    }

    public BigDecimal getEstimatedDiscount() {
        return estimatedDiscount;
    }

    public boolean isBelowMinOrder() {
        return belowMinOrder;
    }

    public boolean isInOtherOrder() {
        return inOtherOrder;
    }

    public boolean isApplied() {
        return applied;
    }

    public boolean isUsable() {
        return !belowMinOrder && !inOtherOrder;
    }

    public boolean isPercent() {
        return VoucherService.PERCENT.equals(discountType);
    }
}
