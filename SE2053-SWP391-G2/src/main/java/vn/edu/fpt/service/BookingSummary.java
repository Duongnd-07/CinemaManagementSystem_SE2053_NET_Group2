package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.util.List;
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.PaymentAttempt;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;

// UC-13 bước 9 trở đi: đơn đã tạo (Pending/Paid/Cancelled) kèm vé, bắp nước và thời gian giữ ghế còn lại
public class BookingSummary {
    private final Booking booking;
    private final Showtime showtime;
    private final Movie movie;
    private final Room room;
    private final List<BookingSeatLine> seats;
    private final List<BookingConcessionLine> concessions;
    private final String voucherCode;
    private final long remainingSeconds;
    // Trạng thái lần thanh toán VNPay gần nhất (UC-14), null nếu khách chưa bấm thanh toán
    private final String paymentStatus;
    // Số điểm hiện tại của khách, để header cập nhật sau khi VNPay báo thanh toán thành công
    private final int pointBalance;

    public BookingSummary(Booking booking, Showtime showtime, Movie movie, Room room, List<BookingSeatLine> seats,
                          List<BookingConcessionLine> concessions, String voucherCode, long remainingSeconds,
                          String paymentStatus, int pointBalance) {
        this.booking = booking;
        this.showtime = showtime;
        this.movie = movie;
        this.room = room;
        this.seats = seats;
        this.concessions = concessions;
        this.voucherCode = voucherCode;
        this.remainingSeconds = remainingSeconds;
        this.paymentStatus = paymentStatus;
        this.pointBalance = pointBalance;
    }

    public Booking getBooking() {
        return booking;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public Movie getMovie() {
        return movie;
    }

    public Room getRoom() {
        return room;
    }

    public List<BookingSeatLine> getSeats() {
        return seats;
    }

    public List<BookingConcessionLine> getConcessions() {
        return concessions;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public int getPointBalance() {
        return pointBalance;
    }

    // Đã chuyển sang VNPay nhưng chưa nhận kết quả (IPN): không cho thanh toán tiếp để tránh trả tiền hai lần
    public boolean isAwaitingGateway() {
        return PaymentAttempt.STATUS_INITIATED.equals(paymentStatus);
    }

    // UC-14 E1: VNPay báo giao dịch thất bại/bị hủy, khách được thanh toán lại khi còn giữ ghế (A1)
    public boolean isLastPaymentFailed() {
        return PaymentAttempt.STATUS_FAILED.equals(paymentStatus);
    }

    // Voucher/điểm trừ hết tiền: chốt đơn không qua VNPay
    public boolean isFree() {
        return booking.getFinalAmount() != null && booking.getFinalAmount().compareTo(BigDecimal.ZERO) == 0;
    }
}
