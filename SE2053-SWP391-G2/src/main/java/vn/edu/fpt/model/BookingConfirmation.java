package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// UC-13/UC-14: nội dung email xác nhận vé gửi cho khách sau khi đơn online được thanh toán
public class BookingConfirmation {
    private int bookingId;
    private String recipientEmail;
    private String recipientName;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private String roomName;
    private String roomFormat;
    private BigDecimal finalAmount;
    private final List<ReminderTicket> tickets = new ArrayList<>();
    // Mỗi dòng dạng "Tên món x2"
    private final List<String> concessions = new ArrayList<>();

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public LocalDateTime getShowtimeStart() {
        return showtimeStart;
    }

    public void setShowtimeStart(LocalDateTime showtimeStart) {
        this.showtimeStart = showtimeStart;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomFormat() {
        return roomFormat;
    }

    public void setRoomFormat(String roomFormat) {
        this.roomFormat = roomFormat;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public List<ReminderTicket> getTickets() {
        return tickets;
    }

    public List<String> getConcessions() {
        return concessions;
    }
}
