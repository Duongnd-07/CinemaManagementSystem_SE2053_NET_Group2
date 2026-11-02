package vn.edu.fpt.model;

import java.time.LocalDateTime;

public class Announcement {
    private int announcementId;
    private int senderId;
    private String title;
    private String message;
    private String notificationType;
    private String targetType;
    private Integer targetUserId;
    private int recipientCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime recalledAt;
    private Integer recalledBy;
    private String senderName;
    private String targetUserName;
    private String targetUserPhone;
    private int readCount;

    public int getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(int value) { announcementId = value; }
    public int getSenderId() { return senderId; }
    public void setSenderId(int value) { senderId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String value) { notificationType = value; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String value) { targetType = value; }
    public Integer getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Integer value) { targetUserId = value; }
    public int getRecipientCount() { return recipientCount; }
    public void setRecipientCount(int value) { recipientCount = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getRecalledAt() { return recalledAt; }
    public void setRecalledAt(LocalDateTime value) { recalledAt = value; }
    public Integer getRecalledBy() { return recalledBy; }
    public void setRecalledBy(Integer value) { recalledBy = value; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String value) { senderName = value; }
    public String getTargetUserName() { return targetUserName; }
    public void setTargetUserName(String value) { targetUserName = value; }
    public String getTargetUserPhone() { return targetUserPhone; }
    public void setTargetUserPhone(String value) { targetUserPhone = value; }
    public int getReadCount() { return readCount; }
    public void setReadCount(int value) { readCount = value; }
    public boolean isRecalled() { return "Recalled".equals(status); }
}
