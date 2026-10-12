package vn.edu.fpt.service;

// Dữ liệu thô (chuỗi) từ form để Service tự validate và hiển thị lại khi có lỗi
public class EquipmentReportForm {
    private String category = "";
    private String roomId = "";
    private String seatId = "";
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

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
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
