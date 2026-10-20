package vn.edu.fpt.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class VerifiedPayment {
    private final String transactionReference;
    private final String providerTransactionId;
    private final boolean successful;
    private final BigDecimal paidAmount;
    private final LocalDateTime paidAt;
    private final String responseCode;

    VerifiedPayment(String transactionReference, String providerTransactionId, boolean successful,
                    BigDecimal paidAmount, LocalDateTime paidAt, String responseCode) {
        this.transactionReference = transactionReference;
        this.providerTransactionId = providerTransactionId;
        this.successful = successful;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
        this.responseCode = responseCode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
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

    public String getResponseCode() {
        return responseCode;
    }
}
