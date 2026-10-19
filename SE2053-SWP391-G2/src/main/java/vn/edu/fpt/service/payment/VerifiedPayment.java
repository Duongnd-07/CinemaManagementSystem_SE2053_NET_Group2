package vn.edu.fpt.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class VerifiedPayment {
    private final String providerTransactionId;
    private final int bookingId;
    private final boolean successful;
    private final BigDecimal paidAmount;
    private final LocalDateTime paidAt;

    VerifiedPayment(String providerTransactionId, int bookingId, boolean successful,
                    BigDecimal paidAmount, LocalDateTime paidAt) {
        this.providerTransactionId = providerTransactionId;
        this.bookingId = bookingId;
        this.successful = successful;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
