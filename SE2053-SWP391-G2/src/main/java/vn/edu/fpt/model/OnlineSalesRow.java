package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// UC-37: report data only; calculations belong to OnlineSalesReportService/DAO.
public class OnlineSalesRow {
    private int id;
    private int bookingId;
    private Integer shiftId;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime showtimeAt;
    private String title;
    private String roomName;
    private String customerName;
    private String description;
    private String status;
    private String reference;
    private String providerTransactionId;
    private String method;
    private String staffName;
    private String warning;
    private int ticketCount;
    private BigDecimal ticketValue = BigDecimal.ZERO;
    private BigDecimal concessionValue = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private BigDecimal cashRefund = BigDecimal.ZERO;
    private BigDecimal voucherAmount = BigDecimal.ZERO;
    private String bookingStatus;
    private BigDecimal bookingFinalAmount;
    private int paidAttemptCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getShowtimeAt() { return showtimeAt; }
    public void setShowtimeAt(LocalDateTime showtimeAt) { this.showtimeAt = showtimeAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getWarning() { return warning; }
    public void setWarning(String warning) { this.warning = warning; }

    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }

    public BigDecimal getTicketValue() { return ticketValue; }
    public void setTicketValue(BigDecimal ticketValue) { this.ticketValue = ticketValue; }

    public BigDecimal getConcessionValue() { return concessionValue; }
    public void setConcessionValue(BigDecimal concessionValue) { this.concessionValue = concessionValue; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }

    public BigDecimal getCashRefund() { return cashRefund; }
    public void setCashRefund(BigDecimal cashRefund) { this.cashRefund = cashRefund; }
    public BigDecimal getNetAmount() { return amount.subtract(cashRefund); }

    public BigDecimal getVoucherAmount() { return voucherAmount; }
    public void setVoucherAmount(BigDecimal voucherAmount) { this.voucherAmount = voucherAmount; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String value) { bookingStatus = value; }
    public BigDecimal getBookingFinalAmount() { return bookingFinalAmount; }
    public void setBookingFinalAmount(BigDecimal value) { bookingFinalAmount = value; }
    public int getPaidAttemptCount() { return paidAttemptCount; }
    public void setPaidAttemptCount(int value) { paidAttemptCount = value; }
}

