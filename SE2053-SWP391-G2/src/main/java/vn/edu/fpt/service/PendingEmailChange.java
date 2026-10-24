package vn.edu.fpt.service;

import java.io.Serializable;
import java.time.LocalDateTime;

// UC-10: Yêu cầu đổi email đang chờ xác thực OTP, lưu trong session của khách hàng
public class PendingEmailChange implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String newEmail;
    private LocalDateTime lastSentAt;
    private int failedAttempts;

    public PendingEmailChange(int userId, String newEmail, LocalDateTime lastSentAt) {
        this.userId = userId;
        this.newEmail = newEmail;
        this.lastSentAt = lastSentAt;
    }

    public int getUserId() {
        return userId;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public LocalDateTime getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(LocalDateTime lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
}
