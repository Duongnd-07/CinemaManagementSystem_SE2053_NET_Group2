package vn.edu.fpt.service;

import java.util.ArrayList;
import java.util.List;

// Dữ liệu thô (chuỗi) từ form để Service tự validate và hiển thị lại khi có lỗi
public class EquipmentReportForm {
    private String category = "";
    private String roomId = "";
    // Nhiều ghế hoặc nhiều thiết bị cụ thể trong một lần gửi, mỗi cái thành một báo cáo riêng
    private List<String> seatIds = new ArrayList<>();
    private List<String> equipmentIds = new ArrayList<>();
    private String urgency = "";
    private String description = "";

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<String> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<String> seatIds) {
        this.seatIds = seatIds;
    }

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public void setEquipmentIds(List<String> equipmentIds) {
        this.equipmentIds = equipmentIds;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
