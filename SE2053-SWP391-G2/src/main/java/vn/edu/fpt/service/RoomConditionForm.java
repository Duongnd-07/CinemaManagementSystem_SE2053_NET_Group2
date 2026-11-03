package vn.edu.fpt.service;

import java.util.HashMap;
import java.util.Map;

// UC-35: dữ liệu thô từ form để Service validate và hiển thị lại khi có lỗi
public class RoomConditionForm {
    public static final String OK = "OK";
    public static final String FAIL = "FAIL";

    private String roomId = "";
    private String note = "";
    // Khóa là RoomCheckItem.getKey()
    private Map<String, String> results = new HashMap<>();
    private Map<String, String> itemNotes = new HashMap<>();

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Map<String, String> getResults() {
        return results;
    }

    public Map<String, String> getItemNotes() {
        return itemNotes;
    }
}
