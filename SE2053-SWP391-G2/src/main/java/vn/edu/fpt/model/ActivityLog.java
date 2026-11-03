package vn.edu.fpt.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// UC-32: một dòng trong bảng ActivityLog
public class ActivityLog {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private long logId;
    private LocalDateTime createdAt;
    private String level;
    private String eventType;
    private String action;
    private Integer userId;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String targetType;
    private String targetId;
    private String description;
    private String ipAddress;

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(TIME_FORMAT);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevelLabel() {
        return LogLevel.labelOf(level);
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTypeLabel() {
        return LogEventType.labelOf(eventType);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    // Tên người thao tác; đăng nhập sai chỉ có email đã nhập; webhook/khách chưa đăng nhập không có ai
    public String getActorDisplay() {
        if (actorName != null && !actorName.isEmpty()) {
            return actorName;
        }
        return actorEmail != null && !actorEmail.isEmpty() ? actorEmail : "Hệ thống / khách";
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    // "Movie #12", "Movie" hoặc rỗng
    public String getTargetText() {
        if (targetType == null || targetType.isEmpty()) {
            return "";
        }
        return targetId == null || targetId.isEmpty() ? targetType : targetType + " #" + targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
