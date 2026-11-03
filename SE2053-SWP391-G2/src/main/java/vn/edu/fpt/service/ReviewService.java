package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import vn.edu.fpt.dao.CustomerNotificationDAO;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.dao.ReviewDAO;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Review;
import vn.edu.fpt.model.ReviewStatus;
import vn.edu.fpt.model.ReviewSummary;
import vn.edu.fpt.model.ReviewView;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

// UC-17: Rate & Review Movie (GB-07)
public class ReviewService {
    public static final int PAGE_SIZE = 5;
    public static final int MANAGER_PAGE_SIZE = 15;
    private static final int RATING_MIN = 1;
    private static final int RATING_MAX = 5;
    private static final int MAX_COMMENT = 1000;
    private static final int MAX_REASON = 255;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();

    public ReviewSummary summarize(int movieId) throws SQLException {
        return reviewDAO.summarize(movieId);
    }

    public Review findMine(int movieId, int userId) throws SQLException {
        return userId > 0 ? reviewDAO.findByMovieAndUser(movieId, userId) : null;
    }

    public Page<ReviewView> listOthers(int movieId, int excludeUserId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, reviewDAO.countVisibleByMovie(movieId, excludeUserId),
                (offset, limit) -> reviewDAO.findVisibleByMovie(movieId, excludeUserId, offset, limit));
    }

    // Phim phải tồn tại, chưa bị Manager ẩn, và người dùng đã xem xong phim (GB-07)
    public boolean canWriteReview(int movieId, int userId) throws SQLException {
        if (userId <= 0) {
            return false;
        }
        Movie movie = movieDAO.findById(movieId);
        if (movie == null || MovieStatus.HIDDEN.equals(movie.getStatus())) {
            return false;
        }
        return reviewDAO.hasWatchedMovie(userId, movieId);
    }

    // true = vừa tạo review mới, false = vừa cập nhật review đã có (GB-07: gửi lại = cập nhật)
    public boolean submit(ReviewForm form, int userId) throws ValidationException, SQLException {
        Movie movie = movieDAO.findById(form.getMovieId());
        if (movie == null || MovieStatus.HIDDEN.equals(movie.getStatus())) {
            throw new ValidationException(Map.of("general", Messages.REVIEW_MOVIE_UNAVAILABLE));
        }
        if (!reviewDAO.hasWatchedMovie(userId, form.getMovieId())) {
            throw new ValidationException(Map.of("general", Messages.REVIEW_NOT_ELIGIBLE));
        }
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Review existing = reviewDAO.findByMovieAndUser(form.getMovieId(), userId);
        if (existing != null && ReviewStatus.HIDDEN.equals(existing.getStatus())) {
            // Review đang bị ẩn vì vi phạm: không cho sửa/gửi lại để tránh lách việc ẩn
            throw new ValidationException(Map.of("general", Messages.REVIEW_HIDDEN_LOCKED));
        }

        Review review = new Review();
        review.setMovieId(form.getMovieId());
        review.setUserId(userId);
        review.setRating(form.getRating());
        review.setComment(blankToNull(form.getComment()));

        if (existing != null) {
            if (!reviewDAO.updateOwnVisible(review)) {
                throw new ValidationException(Map.of("general", Messages.REVIEW_HIDDEN_LOCKED));
            }
            return false;
        }
        try {
            reviewDAO.insert(review);
            return true;
        } catch (SQLException e) {
            // Hai tab gửi review cùng lúc: UNIQUE(MovieID, UserID) chặn ở DB -> chuyển sang cập nhật
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                if (!reviewDAO.updateOwnVisible(review)) {
                    throw new ValidationException(Map.of("general", Messages.REVIEW_HIDDEN_LOCKED));
                }
                return false;
            }
            throw e;
        }
    }

    public void delete(int reviewId, int userId) throws ValidationException, SQLException {
        if (!reviewDAO.deleteOwnVisible(reviewId, userId)) {
            throw new ValidationException(Map.of("general", Messages.REVIEW_NOT_FOUND));
        }
    }

    private Map<String, String> validate(ReviewForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getRating() < RATING_MIN || form.getRating() > RATING_MAX) {
            errors.put("rating", Messages.REVIEW_RATING_INVALID);
        }
        String comment = form.getComment() == null ? "" : form.getComment().trim();
        if (comment.length() > MAX_COMMENT) {
            errors.put("comment", Messages.format(Messages.MSG08, MAX_COMMENT));
        }
        return errors;
    }

    // ===== Manager: xử lý review vi phạm =====

    public Page<ReviewView> listAll(String status, int page) throws SQLException {
        String filter = ReviewStatus.isValid(status) ? status : null;
        return Page.of(page, MANAGER_PAGE_SIZE, reviewDAO.countAll(filter),
                (offset, limit) -> reviewDAO.findAll(filter, offset, limit));
    }

    public Map<String, Integer> countAllByStatus() throws SQLException {
        Map<String, Integer> counts = reviewDAO.countAllByStatus();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String status : ReviewStatus.ALL) {
            result.put(status, counts.getOrDefault(status, 0));
        }
        return result;
    }

    public ReviewView findById(int reviewId) throws SQLException {
        return reviewDAO.findViewById(reviewId);
    }

    public void hide(int reviewId, int managerUserId, String reason) throws ValidationException, SQLException {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(Map.of("reason", Messages.format(Messages.MSG02, "Lý do ẩn")));
        }
        if (trimmed.length() > MAX_REASON) {
            throw new ValidationException(Map.of("reason", Messages.format(Messages.MSG08, MAX_REASON)));
        }
        ReviewView review = reviewDAO.findViewById(reviewId);
        if (review == null) {
            throw new ValidationException(Map.of("general", Messages.REVIEW_NOT_FOUND));
        }
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!reviewDAO.hide(conn, reviewId, managerUserId, trimmed)) {
                    throw new ValidationException(Map.of("general", Messages.REVIEW_NOT_FOUND));
                }
                notificationDAO.insert(conn, review.getUserId(), Messages.REVIEW_NOTIFICATION_HIDDEN_TITLE,
                        Messages.format(Messages.REVIEW_NOTIFICATION_HIDDEN_MESSAGE, review.getMovieTitle(),
                                trimmed),
                        "Review");
                conn.commit();
            } catch (SQLException | ValidationException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public void unhide(int reviewId) throws ValidationException, SQLException {
        try (Connection conn = DBContext.getConnection()) {
            if (!reviewDAO.unhide(conn, reviewId)) {
                throw new ValidationException(Map.of("general", Messages.REVIEW_NOT_FOUND));
            }
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
