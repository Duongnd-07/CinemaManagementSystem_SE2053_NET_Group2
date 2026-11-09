package vn.edu.fpt.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// UC-35: một suất chiếu cùng lần kiểm tra phòng của nó (nếu có). Trạng thái và khung giờ kiểm tra do
// RoomInspectionService tính (BR-35-02); inspectionId = 0 nghĩa là chưa kiểm tra.
public class InspectionSlot {
    public static final String NOT_INSPECTED = "NotInspected";
    public static final String OK = "OK";
    public static final String HAS_ISSUE = "HasIssue";
    public static final String MISSED = "Missed";

    private int showtimeId;
    private int roomId;
    private String roomName;
    private String movieTitle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String showtimeStatus;
    // Giờ kết thúc của suất trước trong cùng phòng, cùng ngày (null nếu là suất đầu ngày)
    private LocalDateTime previousEnd;
    private LocalDateTime windowOpen;

    private int inspectionId;
    private int staffId;
    private String staffName;
    private boolean seatsChecked;
    private boolean roomCleaned;
    private boolean equipmentOk;
    private boolean hasIssue;
    private boolean late;
    private String note;
    private LocalDateTime inspectedAt;
    private LocalDateTime updatedAt;
    private int reportCount;
    private List<LinkedReport> linkedReports = new ArrayList<>();

    private String status;
    private boolean windowOpenNow;
    private boolean urgent;
    private boolean editable;

    public boolean isInspected() {
        return inspectionId > 0;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
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

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getShowtimeStatus() {
        return showtimeStatus;
    }

    public void setShowtimeStatus(String showtimeStatus) {
        this.showtimeStatus = showtimeStatus;
    }

    public LocalDateTime getPreviousEnd() {
        return previousEnd;
    }

    public void setPreviousEnd(LocalDateTime previousEnd) {
        this.previousEnd = previousEnd;
    }

    public LocalDateTime getWindowOpen() {
        return windowOpen;
    }

    public void setWindowOpen(LocalDateTime windowOpen) {
        this.windowOpen = windowOpen;
    }

    public int getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(int inspectionId) {
        this.inspectionId = inspectionId;
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

    public boolean isSeatsChecked() {
        return seatsChecked;
    }

    public void setSeatsChecked(boolean seatsChecked) {
        this.seatsChecked = seatsChecked;
    }

    public boolean isRoomCleaned() {
        return roomCleaned;
    }

    public void setRoomCleaned(boolean roomCleaned) {
        this.roomCleaned = roomCleaned;
    }

    public boolean isEquipmentOk() {
        return equipmentOk;
    }

    public void setEquipmentOk(boolean equipmentOk) {
        this.equipmentOk = equipmentOk;
    }

    public boolean isHasIssue() {
        return hasIssue;
    }

    public void setHasIssue(boolean hasIssue) {
        this.hasIssue = hasIssue;
    }

    public boolean isLate() {
        return late;
    }

    public void setLate(boolean late) {
        this.late = late;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(LocalDateTime inspectedAt) {
        this.inspectedAt = inspectedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    // Đọc chuỗi "44:Resolved,46:Pending" từ DAO thành danh sách báo cáo đã gắn với lần kiểm tra
    public void setReportRefs(String refs) {
        linkedReports = new ArrayList<>();
        if (refs == null || refs.isBlank()) {
            return;
        }
        for (String part : refs.split(",")) {
            int sep = part.indexOf(':');
            if (sep > 0) {
                linkedReports.add(new LinkedReport(Integer.parseInt(part.substring(0, sep).trim()),
                        part.substring(sep + 1).trim()));
            }
        }
    }

    public List<LinkedReport> getLinkedReports() {
        return linkedReports;
    }

    // Báo cáo đầu tiên chưa xử lý xong (để nút "Xử lý sự cố" mở thẳng); 0 nếu đã xử lý hết
    public int getFirstOpenReportId() {
        for (LinkedReport report : linkedReports) {
            if (report.isOpen()) {
                return report.getReportId();
            }
        }
        return 0;
    }

    // Báo cáo sự cố gắn với lần kiểm tra: mã và trạng thái hiện tại (UC-26)
    public static class LinkedReport {
        private final int reportId;
        private final String status;

        public LinkedReport(int reportId, String status) {
            this.reportId = reportId;
            this.status = status;
        }

        public int getReportId() {
            return reportId;
        }

        public String getStatus() {
            return status;
        }

        public boolean isOpen() {
            return !ReportStatus.RESOLVED.equals(status);
        }

        public String getStatusLabel() {
            if (ReportStatus.PENDING.equals(status)) {
                return "Chờ xử lý";
            }
            if (ReportStatus.IN_PROGRESS.equals(status)) {
                return "Đang xử lý";
            }
            return "Đã xử lý";
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isWindowOpenNow() {
        return windowOpenNow;
    }

    public void setWindowOpenNow(boolean windowOpenNow) {
        this.windowOpenNow = windowOpenNow;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
