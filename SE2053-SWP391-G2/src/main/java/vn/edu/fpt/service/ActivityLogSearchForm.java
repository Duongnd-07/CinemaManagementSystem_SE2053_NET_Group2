package vn.edu.fpt.service;

import java.time.LocalDate;

// UC-32: bộ lọc nhật ký do Admin nhập; Service chuẩn hóa trước khi truy vấn và hiển thị lại trên form
public class ActivityLogSearchForm {
    private String fromDate = "";
    private String toDate = "";
    private String level = "";
    private String eventType = "";
    private String user = "";
    private String keyword = "";
    private LocalDate from;
    private LocalDate to;

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    // Ngày đã kiểm tra hợp lệ; null = không giới hạn phía đó
    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}
