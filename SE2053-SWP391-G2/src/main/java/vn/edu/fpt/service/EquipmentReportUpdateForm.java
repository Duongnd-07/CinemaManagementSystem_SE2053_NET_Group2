package vn.edu.fpt.service;

// Dữ liệu thô từ form xử lý báo cáo sự cố (UC-26)
public class EquipmentReportUpdateForm {
    private String newStatus = "";
    private String note = "";
    private boolean lockRoom;
    // Trạng thái báo cáo lúc mở form, để phát hiện Manager khác đã cập nhật trước
    private String currentStatus = "";

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isLockRoom() {
        return lockRoom;
    }

    public void setLockRoom(boolean lockRoom) {
        this.lockRoom = lockRoom;
    }
}
