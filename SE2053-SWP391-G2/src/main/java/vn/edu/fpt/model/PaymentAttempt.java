package vn.edu.fpt.model;

import java.math.BigDecimal;

public class PaymentAttempt {
    public static final String STATUS_INITIATED = "Initiated";
    public static final String STATUS_PAID = "Paid";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_EXPIRED = "Expired";
    public static final String STATUS_REVIEW_REQUIRED = "ReviewRequired";

    private int bookingId;
    private int customerId;
    private String transactionReference;
    private String providerTransactionId;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private String status;
    private String responseCode;

    public PaymentAttempt() {
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getProviderTransactionId() {
        return providerTransactionId;
    }

    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }
}
