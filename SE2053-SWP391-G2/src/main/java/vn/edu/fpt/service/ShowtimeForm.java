package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-24: Form nhập liệu suất chiếu phục vụ validate và hiển thị lại khi có lỗi.
 * Sửa suất chiếu dùng roomId (1 phòng); tạo mới dùng roomIds để tạo cùng lúc ở nhiều phòng.
 */
public class ShowtimeForm {
    private int showtimeId;
    private String movieId = "";
    private String roomId = "";
    private List<String> roomIds = new ArrayList<>();
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

    public List<String> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<String> roomIds) {
        this.roomIds = cleanRoomIds(roomIds);
    }

    // Bỏ ô trống và mã phòng trùng, giữ thứ tự người dùng chọn
    static List<String> cleanRoomIds(List<String> roomIds) {
        List<String> cleaned = new ArrayList<>();
        if (roomIds != null) {
            for (String id : roomIds) {
                String value = id == null ? "" : id.trim();
                if (!value.isEmpty() && !cleaned.contains(value)) {
                    cleaned.add(value);
                }
            }
        }
        return cleaned;
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
