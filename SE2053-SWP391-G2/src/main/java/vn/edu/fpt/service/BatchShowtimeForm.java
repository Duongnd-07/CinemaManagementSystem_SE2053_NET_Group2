package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-24: Form phục vụ tạo lịch chiếu hàng loạt (Batch Showtime Creation)
 */
public class BatchShowtimeForm {
    private String movieId = "";
    private String roomId = "";
    private String startDate = "";
    private String endDate = "";
    private List<Integer> daysOfWeek = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();
    private String basePrice = "";
    private boolean skipCollisions = false;

    public BatchShowtimeForm() {
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId == null ? "" : movieId.trim();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId == null ? "" : roomId.trim();
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate == null ? "" : startDate.trim();
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate == null ? "" : endDate.trim();
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new ArrayList<>();
    }

    public List<String> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<String> timeSlots) {
        this.timeSlots = timeSlots != null ? timeSlots : new ArrayList<>();
    }

    public String getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(String basePrice) {
        this.basePrice = basePrice == null ? "" : basePrice.trim();
    }

    public boolean isSkipCollisions() {
        return skipCollisions;
    }

    public void setSkipCollisions(boolean skipCollisions) {
        this.skipCollisions = skipCollisions;
    }
}
