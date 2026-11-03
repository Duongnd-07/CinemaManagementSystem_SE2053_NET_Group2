package vn.edu.fpt.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Showtime {
    private int showtimeId;
    private int movieId;
    private int roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private String status;
    // Hình thức bản chiếu: "Phụ đề" hoặc "Lồng tiếng" (mặc định "Phụ đề")
    private String translationType = "Phụ đề";
    // Phòng chiếu của suất (nạp kèm khi cần hiển thị tên phòng/định dạng, ví dụ UC-08, UC-24)
    private Room room;
    // Phim của suất chiếu (nạp kèm khi cần hiển thị tên phim/thời lượng, ví dụ UC-24)
    private Movie movie;
    // Số vé đã bán / giữ chỗ
    private int bookedTickets;

    public Showtime() {
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
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

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public int getBookedTickets() {
        return bookedTickets;
    }

    public void setBookedTickets(int bookedTickets) {
        this.bookedTickets = bookedTickets;
    }

    public String getTranslationType() {
        return translationType != null && !translationType.isEmpty() ? translationType : "Phụ đề";
    }

    public void setTranslationType(String translationType) {
        this.translationType = (translationType != null && translationType.trim().equalsIgnoreCase("Lồng tiếng"))
                ? "Lồng tiếng" : "Phụ đề";
    }
}
