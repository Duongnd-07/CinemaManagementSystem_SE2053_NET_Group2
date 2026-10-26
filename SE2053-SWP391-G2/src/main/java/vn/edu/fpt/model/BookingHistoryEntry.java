package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingHistoryEntry {
    private int bookingId;
    private int ticketId;
    private int movieId;
    private String movieTitle;
    private String seatLabel;
    private String ticketCode;
    private String qrCode;
    private String ticketStatus;
    private String bookingStatus;
    private String channel;
    private BigDecimal ticketPrice;
    private BigDecimal finalAmount;
    private LocalDateTime bookingTime;
    private LocalDateTime showtimeStart;
    // UC-17/GB-07: vé đã thanh toán, chưa hủy/đổi, suất chiếu đã kết thúc và phim chưa bị ẩn -> được đánh giá
    private boolean reviewEligible;

    public BookingHistoryEntry() {
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public boolean isReviewEligible() {
        return reviewEligible;
    }

    public void setReviewEligible(boolean reviewEligible) {
        this.reviewEligible = reviewEligible;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public LocalDateTime getShowtimeStart() {
        return showtimeStart;
    }

    public void setShowtimeStart(LocalDateTime showtimeStart) {
        this.showtimeStart = showtimeStart;
    }
}
