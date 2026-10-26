package vn.edu.fpt.service;

import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;

// UC-13 bước 6-8: dữ liệu trang checkout (ghế đã giữ, bắp nước, voucher, điểm, tổng tiền, thời gian giữ còn lại)
public class CheckoutView {
    private final Showtime showtime;
    private final Movie movie;
    private final Room room;
    private final List<BookingSeatLine> seats;
    private final List<BookingConcessionLine> concessions;
    private final BookingQuote quote;
    // Voucher còn hiệu lực trong ví của khách để chọn nhanh
    private final List<CheckoutVoucherOption> vouchers;
    private final int availablePoints;
    private final int pointRate;
    private final long remainingSeconds;
    // Lỗi phát hiện khi tính lại (voucher hết hạn, điểm không còn đủ...) - mục tương ứng đã bị bỏ khỏi đơn
    private final Map<String, String> errors;

    public CheckoutView(Showtime showtime, Movie movie, Room room, List<BookingSeatLine> seats,
                        List<BookingConcessionLine> concessions, BookingQuote quote,
                        List<CheckoutVoucherOption> vouchers, int availablePoints, int pointRate,
                        long remainingSeconds, Map<String, String> errors) {
        this.showtime = showtime;
        this.movie = movie;
        this.room = room;
        this.seats = seats;
        this.concessions = concessions;
        this.quote = quote;
        this.vouchers = vouchers;
        this.availablePoints = availablePoints;
        this.pointRate = pointRate;
        this.remainingSeconds = remainingSeconds;
        this.errors = errors;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public List<CheckoutVoucherOption> getVouchers() {
        return vouchers;
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

    public BookingQuote getQuote() {
        return quote;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public int getPointRate() {
        return pointRate;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
