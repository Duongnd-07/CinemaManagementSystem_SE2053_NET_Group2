package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CancellableTicket {
    private int ticketId;
    private int seatId;
    private int showtimeId;
    private BigDecimal price;
    private String ticketStatus;
    private String bookingStatus;
    private String channel;
    private LocalDateTime showtimeStart;

    public CancellableTicket() {
    }

    public int getTicketId() {
        return ticketId;
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
