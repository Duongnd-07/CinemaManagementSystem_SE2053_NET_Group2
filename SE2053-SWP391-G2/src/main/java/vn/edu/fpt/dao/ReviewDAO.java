package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Review;
import vn.edu.fpt.model.ReviewSummary;
import vn.edu.fpt.model.ReviewView;
import vn.edu.fpt.util.DBContext;

// UC-17: Rate & Review Movie
public class ReviewDAO {
    private static final String VIEW_SELECT = "SELECT r.ReviewID, r.MovieID, m.Title AS MovieTitle, r.UserID, "
            + "u.FullName AS ReviewerName, r.Rating, r.Comment, r.Status, r.HiddenReason, r.HiddenAt, "
            + "hb.FullName AS HiddenByName, r.CreatedAt, r.UpdatedAt "
            + "FROM Review r JOIN Movie m ON m.MovieID = r.MovieID JOIN [User] u ON u.UserID = r.UserID "
            + "LEFT JOIN [User] hb ON hb.UserID = r.HiddenByUserID ";

    // GB-07: chỉ người có vé đã thanh toán, chưa hủy/đổi và suất chiếu đã kết thúc mới được đánh giá
    public boolean hasWatchedMovie(int userId, int movieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
                + "WHERE b.CustomerID = ? AND s.MovieID = ? AND b.Status = 'Paid' "
                + "AND t.Status IN ('Upcoming', 'Watched') AND s.EndTime <= GETDATE()";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public Review findByMovieAndUser(int movieId, int userId) throws SQLException {
        String sql = "SELECT ReviewID, MovieID, UserID, Rating, Comment, Status, HiddenReason, HiddenByUserID, "
                + "HiddenAt, CreatedAt, UpdatedAt FROM Review WHERE MovieID = ? AND UserID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapReview(rs) : null;
            }
        }
    }

    public int countVisibleByMovie(int movieId, int excludeUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Review WHERE MovieID = ? AND Status = 'Visible' AND UserID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // excludeUserId = 0 (Guest/không đăng nhập) không loại trừ ai vì không có UserID = 0
    public List<ReviewView> findVisibleByMovie(int movieId, int excludeUserId, int offset, int limit)
            throws SQLException {
        String sql = VIEW_SELECT + "WHERE r.MovieID = ? AND r.Status = 'Visible' AND r.UserID <> ? "
                + "ORDER BY r.CreatedAt DESC, r.ReviewID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, excludeUserId);
            ps.setInt(3, offset);
            ps.setInt(4, limit);
            return mapViews(ps);
        }
    }

    // Phân bố số sao của phim (chỉ review Visible), dùng tính điểm trung bình ở Service
    public ReviewSummary summarize(int movieId) throws SQLException {
        String sql = "SELECT Rating, COUNT(*) AS Cnt FROM Review WHERE MovieID = ? AND Status = 'Visible' "
                + "GROUP BY Rating";
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt("Rating"), rs.getInt("Cnt"));
                }
            }
        }
        return ReviewSummary.of(counts);
    }

    public void insert(Review review) throws SQLException {
        String sql = "INSERT INTO Review (MovieID, UserID, Rating, Comment, CreatedAt, Status) "
                + "VALUES (?, ?, ?, ?, GETDATE(), 'Visible')";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getMovieId());
            ps.setInt(2, review.getUserId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getComment());
            ps.executeUpdate();
        }
    }

    // Chỉ sửa được review của chính mình khi còn Visible (Hidden thì Manager đang xử lý vi phạm)
    public boolean updateOwnVisible(Review review) throws SQLException {
        String sql = "UPDATE Review SET Rating = ?, Comment = ?, UpdatedAt = GETDATE() "
                + "WHERE MovieID = ? AND UserID = ? AND Status = 'Visible'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, review.getRating());
            ps.setString(2, review.getComment());
            ps.setInt(3, review.getMovieId());
            ps.setInt(4, review.getUserId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteOwnVisible(int reviewId, int userId) throws SQLException {
        String sql = "DELETE FROM Review WHERE ReviewID = ? AND UserID = ? AND Status = 'Visible'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    // ===== Quản lý review vi phạm (Manager) =====

    public int countAll(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Review" + (status == null ? "" : " WHERE Status = ?");
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> countAllByStatus() throws SQLException {
        String sql = "SELECT Status, COUNT(*) AS Cnt FROM Review GROUP BY Status";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("Status"), rs.getInt("Cnt"));
            }
        }
        return counts;
    }

    public List<ReviewView> findAll(String status, int offset, int limit) throws SQLException {
        String sql = VIEW_SELECT + (status == null ? "" : "WHERE r.Status = ? ")
                + "ORDER BY r.CreatedAt DESC, r.ReviewID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (status != null) {
                ps.setString(idx++, status);
            }
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            return mapViews(ps);
        }
    }

    public ReviewView findViewById(int reviewId) throws SQLException {
        String sql = VIEW_SELECT + "WHERE r.ReviewID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapView(rs) : null;
            }
        }
    }

    public boolean hide(Connection conn, int reviewId, int managerUserId, String reason) throws SQLException {
        String sql = "UPDATE Review SET Status = 'Hidden', HiddenReason = ?, HiddenByUserID = ?, "
                + "HiddenAt = GETDATE() WHERE ReviewID = ? AND Status = 'Visible'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, managerUserId);
            ps.setInt(3, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean unhide(Connection conn, int reviewId) throws SQLException {
        String sql = "UPDATE Review SET Status = 'Visible', HiddenReason = NULL, HiddenByUserID = NULL, "
                + "HiddenAt = NULL, UpdatedAt = GETDATE() WHERE ReviewID = ? AND Status = 'Hidden'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewId);
            return ps.executeUpdate() == 1;
        }
    }

    private List<ReviewView> mapViews(PreparedStatement ps) throws SQLException {
        List<ReviewView> views = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                views.add(mapView(rs));
            }
        }
        return views;
    }

    private ReviewView mapView(ResultSet rs) throws SQLException {
        ReviewView view = new ReviewView();
        view.setReviewId(rs.getInt("ReviewID"));
        view.setMovieId(rs.getInt("MovieID"));
        view.setMovieTitle(rs.getString("MovieTitle"));
        view.setUserId(rs.getInt("UserID"));
        view.setReviewerName(rs.getString("ReviewerName"));
        view.setRating(rs.getInt("Rating"));
        view.setComment(rs.getString("Comment"));
        view.setStatus(rs.getString("Status"));
        view.setHiddenReason(rs.getString("HiddenReason"));
        view.setHiddenByName(rs.getString("HiddenByName"));
        Timestamp hiddenAt = rs.getTimestamp("HiddenAt");
        view.setHiddenAt(hiddenAt == null ? null : hiddenAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        view.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        view.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return view;
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("ReviewID"));
        review.setMovieId(rs.getInt("MovieID"));
        review.setUserId(rs.getInt("UserID"));
        review.setRating(rs.getInt("Rating"));
        review.setComment(rs.getString("Comment"));
        review.setStatus(rs.getString("Status"));
        review.setHiddenReason(rs.getString("HiddenReason"));
        review.setHiddenByUserId(rs.getObject("HiddenByUserID", Integer.class));
        Timestamp hiddenAt = rs.getTimestamp("HiddenAt");
        review.setHiddenAt(hiddenAt == null ? null : hiddenAt.toLocalDateTime());
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        review.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        review.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return review;
    }
}
