package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancellableTicket {
    private int ticketId;
    private int bookingId;
    private int seatId;
    private int showtimeId;
    private BigDecimal price;
    private BigDecimal bookingTicketTotal;
    private int bookingPointsEarned;
    private int upcomingTicketCount;
    private String ticketStatus;
    private String bookingStatus;
    private String channel;
    private LocalDateTime showtimeStart;

    public CancellableTicket() {
    }

    public int getTicketId() {
        return ticketId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getBookingTicketTotal() {
        return bookingTicketTotal;
    }

    public void setBookingTicketTotal(BigDecimal bookingTicketTotal) {
        this.bookingTicketTotal = bookingTicketTotal;
    }

    public int getBookingPointsEarned() {
        return bookingPointsEarned;
    }

    public void setBookingPointsEarned(int bookingPointsEarned) {
        this.bookingPointsEarned = bookingPointsEarned;
    }

    public int getUpcomingTicketCount() {
        return upcomingTicketCount;
    }

    public void setUpcomingTicketCount(int upcomingTicketCount) {
        this.upcomingTicketCount = upcomingTicketCount;
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

    public LocalDateTime getShowtimeStart() {
        return showtimeStart;
    }

    public void setShowtimeStart(LocalDateTime showtimeStart) {
        this.showtimeStart = showtimeStart;
    }
}
