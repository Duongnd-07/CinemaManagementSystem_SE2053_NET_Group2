package vn.edu.fpt.service;

/**
 * UC-24: Form nhập liệu suất chiếu phục vụ validate và hiển thị lại khi có lỗi.
 */
public class ShowtimeForm {
    private int showtimeId;
    private String movieId = "";
    private String roomId = "";
    private String startTime = "";
    private String basePrice = "";
    private String status = "Active";
    private String translationType = "Phụ đề";

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime == null ? "" : startTime.trim();
    }

    public String getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(String basePrice) {
        this.basePrice = basePrice == null ? "" : basePrice.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "Active" : status.trim();
    }

    public String getTranslationType() {
        return translationType;
    }

    public void setTranslationType(String translationType) {
        this.translationType = (translationType != null && translationType.trim().equalsIgnoreCase("Lồng tiếng"))
                ? "Lồng tiếng" : "Phụ đề";
    }
}
