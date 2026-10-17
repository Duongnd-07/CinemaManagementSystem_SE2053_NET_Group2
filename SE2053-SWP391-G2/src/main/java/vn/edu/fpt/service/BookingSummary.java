package vn.edu.fpt.service;

import java.util.List;
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.Movie;
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

    public BookingSummary(Booking booking, Showtime showtime, Movie movie, Room room, List<BookingSeatLine> seats,
                          List<BookingConcessionLine> concessions, String voucherCode, long remainingSeconds) {
        this.booking = booking;
        this.showtime = showtime;
        this.movie = movie;
        this.room = room;
        this.seats = seats;
        this.concessions = concessions;
        this.voucherCode = voucherCode;
        this.remainingSeconds = remainingSeconds;
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
}
