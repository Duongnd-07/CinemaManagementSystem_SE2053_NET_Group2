package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-24: Form phục vụ tạo lịch chiếu hàng loạt (Batch Showtime Creation).
 * Lịch chia thành nhiều đợt: mỗi đợt là một phòng và một khoảng ngày, một phòng có thể có nhiều đợt
 * (ví dụ phòng 1 chiếu 01–07, nghỉ, rồi 15–30). Thứ trong tuần, khung giờ, giá và bản chiếu dùng chung cho mọi đợt.
 */
public class BatchShowtimeForm {

    /** Một đợt chiếu: phòng + khoảng ngày (chuỗi thô từ form, service kiểm tra và đổi kiểu). */
    public record Period(String roomId, String startDate, String endDate) {
    }

    private String movieId = "";
    private List<Period> periods = new ArrayList<>();
    private List<Integer> daysOfWeek = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();
    private String basePrice = "";
    private boolean skipCollisions = false;
    private String translationType = "Phụ đề";

    public BatchShowtimeForm() {
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId == null ? "" : movieId.trim();
    }

    public List<Period> getPeriods() {
        return periods;
    }

    /**
     * Ghép các cột của bảng đợt chiếu theo cùng chỉ số (periodRoomId[i], periodStart[i], periodEnd[i]).
     * Dòng để trống cả ba ô bị bỏ qua; dòng thiếu một phần vẫn giữ để service báo lỗi đúng đợt.
     */
    public void setPeriods(List<String> roomIds, List<String> startDates, List<String> endDates) {
        periods = new ArrayList<>();
        int rows = Math.max(size(roomIds), Math.max(size(startDates), size(endDates)));
        for (int i = 0; i < rows; i++) {
            String roomId = valueAt(roomIds, i);
            String start = valueAt(startDates, i);
            String end = valueAt(endDates, i);
            if (!roomId.isEmpty() || !start.isEmpty() || !end.isEmpty()) {
                periods.add(new Period(roomId, start, end));
            }
        }
    }

    /** Mã các phòng có trong các đợt, không trùng, theo thứ tự xuất hiện. */
    public List<String> getRoomIds() {
        List<String> ids = new ArrayList<>();
        for (Period period : periods) {
            ids.add(period.roomId());
        }
        return ShowtimeForm.cleanRoomIds(ids);
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

    public String getTranslationType() {
        return translationType;
    }

    public void setTranslationType(String translationType) {
        this.translationType = (translationType != null && translationType.trim().equalsIgnoreCase("Lồng tiếng"))
                ? "Lồng tiếng" : "Phụ đề";
    }

    private static int size(List<String> values) {
        return values == null ? 0 : values.size();
    }

    private static String valueAt(List<String> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index).trim();
    }
}
