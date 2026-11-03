package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Lịch sử bán tại quầy: một đơn kèm khách, vé, giảm giá, tiền thu / thối / hoàn
public class CounterOrder {
    private int bookingId;
    private LocalDateTime bookingTime;
    private String status;
    private Integer shiftId;
    private String counterNo;
    private String staffName;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private String voucherCode;
    private int pointsRedeemed;
    private int pointsEarned;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentReference;
    private BigDecimal cashReceived;
    private BigDecimal refundTotal = BigDecimal.ZERO;
    private final List<Ticket> tickets = new ArrayList<>();

    // Một vé của đơn: ghế, mã vé, trạng thái, tiền đã hoàn (hủy vé / đổi xuống ghế thường)
    public static final class Ticket {
        private final String seat;
        private final String ticketCode;
        private final String status;
        private final BigDecimal refundAmount;

        public Ticket(String seat, String ticketCode, String status, BigDecimal refundAmount) {
            this.seat = seat;
            this.ticketCode = ticketCode;
            this.status = status;
            this.refundAmount = refundAmount;
        }

        public String getSeat() {
            return seat;
        }

        public String getTicketCode() {
            return ticketCode;
        }

        public String getStatus() {
            return status;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }
    }

    // Tiền thối = tiền khách đưa - thực thu; chỉ có với đơn tiền mặt bán sau khi lưu CashReceived
    public BigDecimal getChangeDue() {
        return cashReceived == null || finalAmount == null ? null : cashReceived.subtract(finalAmount);
    }

    public boolean isMember() {
        return customerId != null;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public String getCounterNo() {
        return counterNo;
    }

    public void setCounterNo(String counterNo) {
        this.counterNo = counterNo;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public int getPointsRedeemed() {
        return pointsRedeemed;
    }

    public void setPointsRedeemed(int pointsRedeemed) {
        this.pointsRedeemed = pointsRedeemed;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived) {
        this.cashReceived = cashReceived;
    }

    public BigDecimal getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(BigDecimal refundTotal) {
        this.refundTotal = refundTotal;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}
