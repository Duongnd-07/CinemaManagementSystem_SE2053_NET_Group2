package vn.edu.fpt.model;

import java.time.LocalDateTime;

// UC-36 (A7): một suất chiếu đang mở cửa soát vé (từ 30 phút trước giờ chiếu đến khi kết thúc), kèm số vé đã vào cửa
public class DoorShowtime {
    private int showtimeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String movieTitle;
    private String roomName;
    private String roomFormat;
    // Vé hợp lệ của suất (đơn Paid, vé Upcoming) và số vé đã vào cửa
    private int ticketCount;
    private int checkedInCount;

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
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

    public int getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(int ticketCount) {
        this.ticketCount = ticketCount;
    }

    public int getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(int checkedInCount) {
        this.checkedInCount = checkedInCount;
    }
}
