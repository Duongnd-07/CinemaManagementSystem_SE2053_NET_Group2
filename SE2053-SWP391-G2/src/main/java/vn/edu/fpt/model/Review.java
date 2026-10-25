package vn.edu.fpt.model;

import java.time.LocalDateTime;

public class Review {
    private int reviewId;
    private int movieId;
    private int userId;
    private int rating;
    private String comment;
    private String status;
    private String hiddenReason;
    private Integer hiddenByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime hiddenAt;

    public Review() {
    }

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHiddenReason() {
        return hiddenReason;
    }

    public void setHiddenReason(String hiddenReason) {
        this.hiddenReason = hiddenReason;
    }

    public Integer getHiddenByUserId() {
        return hiddenByUserId;
    }

    public void setHiddenByUserId(Integer hiddenByUserId) {
        this.hiddenByUserId = hiddenByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }
}
