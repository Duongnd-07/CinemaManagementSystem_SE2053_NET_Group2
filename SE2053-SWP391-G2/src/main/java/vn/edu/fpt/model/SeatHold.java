package vn.edu.fpt.model;

import java.time.LocalDateTime;

public class SeatHold {
    private int holdId;
    private String sessionId;
    private Integer userId;
    private int showtimeId;
    private int seatId;
    private LocalDateTime holdTime;
    private LocalDateTime expiryTime;

    public SeatHold() {
    }

    public int getHoldId() {
        return holdId;
    }

    public void setHoldId(int holdId) {
        this.holdId = holdId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public LocalDateTime getHoldTime() {
        return holdTime;
    }

    public void setHoldTime(LocalDateTime holdTime) {
        this.holdTime = holdTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }
}
