package vn.edu.fpt.service;

import java.io.Serializable;

// UC-17: dữ liệu form gửi/sửa đánh giá phim (lưu tạm trong session làm bản nháp khi gửi lỗi)
public class ReviewForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private int movieId;
    private int rating;
    private String comment = "";

    public ReviewForm() {
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
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
}
