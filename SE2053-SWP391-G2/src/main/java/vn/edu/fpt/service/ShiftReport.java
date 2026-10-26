package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.Shift;

// UC-22: báo cáo một ca bán tại quầy. Ca đang mở thì số liệu tính trực tiếp từ đơn; ca đã kết thì lấy số đã chốt
// trong bảng Shift (BR: số liệu đã chốt không đổi dù dữ liệu đơn thay đổi về sau)
public class ShiftReport {
    private final Shift shift;
    private final LocalDateTime plannedEnd;
    private int orderCount;
    private int ticketCount;
    private int concessionItems;
    private BigDecimal ticketRevenue = BigDecimal.ZERO;
    private BigDecimal concessionRevenue = BigDecimal.ZERO;
    private BigDecimal cashTotal = BigDecimal.ZERO;
    private BigDecimal cardTotal = BigDecimal.ZERO;
    private BigDecimal transferTotal = BigDecimal.ZERO;
    private BigDecimal refundTotal = BigDecimal.ZERO;
    // Tổng voucher + điểm của các đơn, đọc từ Booking.DiscountAmount
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private final List<Order> orders = new ArrayList<>();

    public ShiftReport(Shift shift, LocalDateTime plannedEnd) {
        this.shift = shift;
        this.plannedEnd = plannedEnd;
    }

    // Một đơn bán trong ca
    public static final class Order {
        private final int bookingId;
        private final LocalDateTime bookingTime;
        private final String seats;
        private final String customerName;
        private final String paymentMethod;
        // Mã chuẩn chi POS / mã giao dịch ngân hàng để đối chiếu khi kết ca; null với tiền mặt
        private final String paymentReference;
        private final BigDecimal finalAmount;

        public Order(int bookingId, LocalDateTime bookingTime, String seats, String customerName,
                     String paymentMethod, String paymentReference, BigDecimal finalAmount) {
            this.bookingId = bookingId;
            this.bookingTime = bookingTime;
            this.seats = seats;
            this.customerName = customerName;
            this.paymentMethod = paymentMethod;
            this.paymentReference = paymentReference;
            this.finalAmount = finalAmount;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public int getBookingId() {
            return bookingId;
        }

        public LocalDateTime getBookingTime() {
            return bookingTime;
        }

        public String getSeats() {
            return seats;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }
    }

    public Shift getShift() {
        return shift;
    }

    // Giờ ca tự kết nếu nhân viên không kết sớm hơn
    public LocalDateTime getPlannedEnd() {
        return plannedEnd;
    }

    public boolean isClosed() {
        return shift.getEndedAt() != null;
    }

    public boolean isAutoClosed() {
        return isClosed() && shift.getClosedBy() == null;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public int getConcessionItems() {
        return concessionItems;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue;
    }

    public BigDecimal getConcessionRevenue() {
        return concessionRevenue;
    }

    public BigDecimal getGrossRevenue() {
        return ticketRevenue.add(concessionRevenue);
    }

    public BigDecimal getCashTotal() {
        return cashTotal;
    }

    public BigDecimal getCardTotal() {
        return cardTotal;
    }

    public BigDecimal getTransferTotal() {
        return transferTotal;
    }

    public BigDecimal getRefundTotal() {
        return refundTotal;
    }

    // Tổng tiền thực thu theo các hình thức thanh toán
    public BigDecimal getCollectedTotal() {
        return cashTotal.add(cardTotal).add(transferTotal);
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    // BR-02: tiền thu theo hình thức thanh toán phải khớp doanh thu (vé + bắp nước - giảm giá)
    public boolean isReconciled() {
        return getGrossRevenue().subtract(discountTotal).compareTo(getCollectedTotal()) == 0;
    }

    // Tiền mặt đáng lẽ có trong két = tiền đầu ca + tiền mặt thu - tiền mặt hoàn
    public BigDecimal getExpectedCash() {
        return shift.getOpeningCash().add(cashTotal).subtract(refundTotal);
    }

    public boolean isCounted() {
        return shift.getCountedCash() != null;
    }

    // Dương = thừa, âm = thiếu; null khi chưa đếm két
    public BigDecimal getCashDifference() {
        return isCounted() ? shift.getCountedCash().subtract(getExpectedCash()) : null;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public void setConcessionItems(int concessionItems) {
        this.concessionItems = concessionItems;
    }

    public void setTicketRevenue(BigDecimal ticketRevenue) {
        this.ticketRevenue = ticketRevenue;
    }

    public void setConcessionRevenue(BigDecimal concessionRevenue) {
        this.concessionRevenue = concessionRevenue;
    }

    public void setCashTotal(BigDecimal cashTotal) {
        this.cashTotal = cashTotal;
    }

    public void setCardTotal(BigDecimal cardTotal) {
        this.cardTotal = cardTotal;
    }

    public void setTransferTotal(BigDecimal transferTotal) {
        this.transferTotal = transferTotal;
    }

    public void setRefundTotal(BigDecimal refundTotal) {
        this.refundTotal = refundTotal;
    }

    public void setDiscountTotal(BigDecimal discountTotal) {
        this.discountTotal = discountTotal;
    }
}
