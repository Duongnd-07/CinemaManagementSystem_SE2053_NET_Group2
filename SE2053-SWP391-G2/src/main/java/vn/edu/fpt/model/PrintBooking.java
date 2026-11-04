package vn.edu.fpt.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// UC-36: đơn đặt vé tìm được khi quét mã, gồm suất chiếu, các vé và bắp nước của đơn.
// Cũng là nội dung một lượt in (tickets là các vé giấy vừa in, concessionSlip = có in phiếu nhận bắp nước).
public class PrintBooking implements Serializable {
    private static final long serialVersionUID = 1L;

    private int bookingId;
    private String bookingStatus;
    private String channel;
    private String customerName;
    private int showtimeId;
    private String showtimeStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String movieTitle;
    private String ageRating;
    private String roomName;
    private String roomFormat;
    private List<PrintTicket> tickets = new ArrayList<>();
    // Tên + số lượng, vd "Combo Bắp Nước Lớn x 2"
    private List<String> concessions = new ArrayList<>();
    private boolean concessionSlip;
    // Lỗi chung của cả đơn (E2, E4); null nếu đơn in được
    private String blockReason;

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getShowtimeStatus() {
        return showtimeStatus;
    }

    public void setShowtimeStatus(String showtimeStatus) {
        this.showtimeStatus = showtimeStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
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

    public List<PrintTicket> getTickets() {
        return tickets;
    }

    public void setTickets(List<PrintTicket> tickets) {
        this.tickets = tickets;
    }

    public List<String> getConcessions() {
        return concessions;
    }

    public void setConcessions(List<String> concessions) {
        this.concessions = concessions;
    }

    public boolean isConcessionSlip() {
        return concessionSlip;
    }

    public void setConcessionSlip(boolean concessionSlip) {
        this.concessionSlip = concessionSlip;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public boolean isCounterSale() {
        return BookingChannel.COUNTER.equals(channel);
    }

    public boolean isAnyPrintable() {
        return tickets.stream().anyMatch(PrintTicket::isPrintable);
    }
}
