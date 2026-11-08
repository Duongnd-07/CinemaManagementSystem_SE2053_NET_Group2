package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// UC-19: hóa đơn của một đơn bán tại quầy để in cho khách
public class CounterReceipt {
    private int bookingId;
    private LocalDateTime bookingTime;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private String staffName;
    private String counterNo;
    private String customerName;
    private String paymentMethod;
    private String voucherCode;
    private String paymentReference;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private int pointsRedeemed;
    private int pointsEarned;
    private final List<Line> tickets = new ArrayList<>();
    private final List<Line> concessions = new ArrayList<>();
    // Ghế còn vào phòng được (gồm ghế cấp lại khi đổi), chỉ khác ghế lúc bán khi đã đổi / hoàn vé
    private final List<String> usableSeats = new ArrayList<>();

    // Một dòng hóa đơn: vé bán ra (name = ghế, code = mã vé, note = đã đổi / đã hoàn) hoặc món bắp nước
    public static final class Line {
        private final String name;
        private final String code;
        private final int quantity;
        private final BigDecimal amount;
        private final String note;

        public Line(String name, String code, int quantity, BigDecimal amount, String note) {
            this.name = name;
            this.code = code;
            this.quantity = quantity;
            this.amount = amount;
            this.note = note;
        }

        public String getNote() {
            return note;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }
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

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getCounterNo() {
        return counterNo;
    }

    public void setCounterNo(String counterNo) {
        this.counterNo = counterNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    // Mã chuẩn chi POS / mã giao dịch ngân hàng; null với tiền mặt
    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
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

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
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

    public List<Line> getTickets() {
        return tickets;
    }

    public List<String> getUsableSeats() {
        return usableSeats;
    }

    // Có vé đã đổi / đã hoàn thì hóa đơn ghi thêm danh sách ghế còn dùng được
    public boolean isAdjusted() {
        return tickets.stream().anyMatch(t -> t.getNote() != null);
    }

    public List<Line> getConcessions() {
        return concessions;
    }
}
