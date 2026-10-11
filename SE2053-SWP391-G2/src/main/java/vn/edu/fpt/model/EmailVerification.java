package vn.edu.fpt.model;

import java.time.LocalDateTime;

// Ánh xạ bảng EmailVerification (UC-01)
public class EmailVerification {
    private int verificationId;
    private String email;
    private String otpCode;
    private LocalDateTime expiryTime;
    private boolean isUsed;
    private LocalDateTime createdAt;

    public EmailVerification() {
    }

    public int getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(int verificationId) {
        this.verificationId = verificationId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
