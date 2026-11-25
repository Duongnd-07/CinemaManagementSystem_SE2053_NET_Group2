package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-24: Form phục vụ tạo lịch chiếu hàng loạt (Batch Showtime Creation).
 * Lịch chia thành nhiều đợt: mỗi đợt là một phòng, một khoảng ngày, một bản chiếu và một giá riêng; một phòng có thể
 * có nhiều đợt (ví dụ phòng 1 chiếu 01–07 phụ đề 50.000đ, rồi 15–30 lồng tiếng 45.000đ).
 * Thứ trong tuần và khung giờ dùng chung cho mọi đợt.
 */
public class BatchShowtimeForm {

    /**
     * Một đợt chiếu: phòng + khoảng ngày + bản chiếu + giá (chuỗi thô từ form, service kiểm tra và đổi kiểu).
     * Giá rỗng = mỗi suất tự tính giá gợi ý theo ngày thường/cuối tuần, khung giờ và định dạng phòng.
     */
    public record Period(String roomId, String startDate, String endDate, String translationType, String price) {
    }

    private String movieId = "";
    private List<Period> periods = new ArrayList<>();
    private List<Integer> daysOfWeek = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();
    private boolean skipCollisions = false;

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
     * Ghép các cột của bảng đợt chiếu theo cùng chỉ số: periodRoomId[i], periodStart[i], periodEnd[i],
     * periodTranslationType[i], periodPrice[i]. Dòng chưa có phòng và ngày bị bỏ qua; dòng thiếu một phần vẫn giữ
     * để service báo lỗi đúng đợt. Thiếu bản chiếu thì là Phụ đề, thiếu giá thì tự tính.
     */
    public void setPeriods(List<String> roomIds, List<String> startDates, List<String> endDates,
                           List<String> translationTypes, List<String> prices) {
        periods = new ArrayList<>();
        int rows = Math.max(size(roomIds), Math.max(size(startDates), size(endDates)));
        for (int i = 0; i < rows; i++) {
            String roomId = valueAt(roomIds, i);
            String start = valueAt(startDates, i);
            String end = valueAt(endDates, i);
            if (!roomId.isEmpty() || !start.isEmpty() || !end.isEmpty()) {
                periods.add(new Period(roomId, start, end, normalizeTranslationType(valueAt(translationTypes, i)),
                        valueAt(prices, i)));
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

    /** Khung giờ dạng "HH:mm", bỏ ô trống. */
    public void setTimeSlots(List<String> timeSlots) {
        this.timeSlots = new ArrayList<>();
        if (timeSlots != null) {
            for (String slot : timeSlots) {
                if (slot != null && !slot.trim().isEmpty()) {
                    this.timeSlots.add(slot.trim());
                }
            }
        }
    }

    public boolean isSkipCollisions() {
        return skipCollisions;
    }

    public void setSkipCollisions(boolean skipCollisions) {
        this.skipCollisions = skipCollisions;
    }

    // BR-24-09: chỉ có Phụ đề hoặc Lồng tiếng, giá trị lạ thành Phụ đề
    private static String normalizeTranslationType(String translationType) {
        return translationType.equalsIgnoreCase("Lồng tiếng") ? "Lồng tiếng" : "Phụ đề";
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
