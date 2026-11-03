package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// UC-20: một lần đổi ghế / hoàn tiền đã thực hiện, để in phiếu cho khách
public class ExchangeRecord {
    private int exchangeId;
    private String exchangeType;
    private String reason;
    private BigDecimal cashRefund;
    private int pointsReturned;
    private String voucherCode;
    private BigDecimal voucherValue;
    private LocalDateTime voucherExpiry;
    private LocalDateTime createdAt;
    private String staffName;
    private String customerName;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private String oldTicketCode;
    private String oldSeat;
    private final List<String[]> newTickets = new ArrayList<>();

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(String exchangeType) {
        this.exchangeType = exchangeType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getCashRefund() {
        return cashRefund;
    }

    public void setCashRefund(BigDecimal cashRefund) {
        this.cashRefund = cashRefund;
    }

    public int getPointsReturned() {
        return pointsReturned;
    }

    public void setPointsReturned(int pointsReturned) {
        this.pointsReturned = pointsReturned;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public BigDecimal getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(BigDecimal voucherValue) {
        this.voucherValue = voucherValue;
    }

    public LocalDateTime getVoucherExpiry() {
        return voucherExpiry;
    }

    public void setVoucherExpiry(LocalDateTime voucherExpiry) {
        this.voucherExpiry = voucherExpiry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getOldTicketCode() {
        return oldTicketCode;
    }

    public void setOldTicketCode(String oldTicketCode) {
        this.oldTicketCode = oldTicketCode;
    }

    public String getOldSeat() {
        return oldSeat;
    }

    public void setOldSeat(String oldSeat) {
        this.oldSeat = oldSeat;
    }

    // Mỗi phần tử: {ghế, mã vé}
    public List<String[]> getNewTickets() {
        return newTickets;
    }
}
