package vn.edu.fpt.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;

// UC-08: lịch chiếu của một phim đã lọc và nhóm sẵn để JSP hiển thị
public class ShowtimeSchedule {
    // Các ngày trong khung xem (hôm nay + 7 ngày tới)
    private List<LocalDate> days = new ArrayList<>();
    // Số suất của từng ngày, đã áp bộ lọc định dạng/phòng (để hiện trên thanh chọn ngày)
    private Map<LocalDate, Integer> countByDay = new LinkedHashMap<>();
    // Lựa chọn lọc đã chuẩn hóa; null/rỗng = không lọc
    private LocalDate selectedDate;
    private String selectedFormat = "";
    private int selectedRoomId;
    private String selectedTranslationType = "";
    // Các định dạng/phòng/bản chiếu có suất trong khung xem, làm lựa chọn cho bộ lọc
    private List<String> formats = new ArrayList<>();
    private List<Room> rooms = new ArrayList<>();
    private List<String> translationTypes = new ArrayList<>();
    // Ngày -> Định dạng -> Tên phòng -> các suất (đã sắp theo giờ)
    private Map<LocalDate, Map<String, Map<String, List<Showtime>>>> groups = new LinkedHashMap<>();

    public List<LocalDate> getDays() {
        return days;
    }

    public void setDays(List<LocalDate> days) {
        this.days = days;
    }

    public Map<LocalDate, Integer> getCountByDay() {
        return countByDay;
    }

    public void setCountByDay(Map<LocalDate, Integer> countByDay) {
        this.countByDay = countByDay;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedFormat() {
        return selectedFormat;
    }

    public void setSelectedFormat(String selectedFormat) {
        this.selectedFormat = selectedFormat;
    }

    public int getSelectedRoomId() {
        return selectedRoomId;
    }

    public void setSelectedRoomId(int selectedRoomId) {
        this.selectedRoomId = selectedRoomId;
    }

    public List<String> getFormats() {
        return formats;
    }

    public void setFormats(List<String> formats) {
        this.formats = formats;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public String getSelectedTranslationType() {
        return selectedTranslationType;
    }

    public void setSelectedTranslationType(String selectedTranslationType) {
        this.selectedTranslationType = selectedTranslationType;
    }

    public List<String> getTranslationTypes() {
        return translationTypes;
    }

    public void setTranslationTypes(List<String> translationTypes) {
        this.translationTypes = translationTypes;
    }

    public Map<LocalDate, Map<String, Map<String, List<Showtime>>>> getGroups() {
        return groups;
    }

    public void setGroups(Map<LocalDate, Map<String, Map<String, List<Showtime>>>> groups) {
        this.groups = groups;
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }
}
