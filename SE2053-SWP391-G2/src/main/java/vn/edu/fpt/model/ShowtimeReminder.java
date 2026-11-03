package vn.edu.fpt.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeReminder {
    private int bookingId;
    private String recipientEmail;
    private String recipientName;
    private String movieTitle;
    private LocalDateTime showtimeStart;
    private final List<ReminderTicket> tickets = new ArrayList<>();

    public ShowtimeReminder() {
    }

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

    public List<ReminderTicket> getTickets() {
        return List.copyOf(tickets);
    }

    public void addTicket(ReminderTicket ticket) {
        tickets.add(ticket);
    }
}
