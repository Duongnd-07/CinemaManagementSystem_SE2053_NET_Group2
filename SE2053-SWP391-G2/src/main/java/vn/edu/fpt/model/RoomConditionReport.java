package vn.edu.fpt.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// UC-35: báo cáo tình trạng một phòng chiếu do nhân viên lập trước khi mở ca
public class RoomConditionReport {
    public static final String GOOD = "Good";
    public static final String ISSUE = "Issue";

    private static final DateTimeFormatter CREATED_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private int reportId;
    private int roomId;
    private String roomName;
    private int staffId;
    private String staffName;
    private String result;
    private String note;
    private LocalDateTime createdAt;
    private List<RoomConditionItem> items = new ArrayList<>();

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RoomConditionItem> getItems() {
        return items;
    }

    public void setItems(List<RoomConditionItem> items) {
        this.items = items;
    }

    public boolean isIssue() {
        return ISSUE.equals(result);
    }

    public List<RoomConditionItem> getFailedItems() {
        return items.stream().filter(item -> !item.isOk()).toList();
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(CREATED_FORMAT);
    }

    public String getCreatedTimeText() {
        return createdAt == null ? "" : createdAt.format(TIME_FORMAT);
    }
}
