package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.ReviewForm;
import vn.edu.fpt.service.ReviewService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-17: Rate & Review Movie - viết/sửa/xóa đánh giá của chính khách hàng.
// Phim còn công khai (Now Showing/Coming Soon) dùng khung đánh giá ngay ở /movies/detail;
// servlet này phục vụ phim đã Ended (không còn trang chi tiết công khai) nhưng khách đã xem vẫn được đánh giá.
@WebServlet("/customer/reviews")
public class MovieReviewServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieReviewServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/movie-review.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final MovieService movieService = new MovieService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int movieId = parseInt(request.getParameter("movieId"));
        if (movieId < 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            Movie movie = movieService.findById(movieId);
            if (movie == null || MovieStatus.HIDDEN.equals(movie.getStatus())) {
                redirectWithError(request, response, Messages.REVIEW_MOVIE_UNAVAILABLE);
                return;
            }
            if (MovieStatus.PUBLIC.contains(movie.getStatus())) {
                response.sendRedirect(request.getContextPath() + "/movies/detail?id=" + movieId + "#reviews");
                return;
            }
            render(request, response, movie);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int movieId = parseInt(request.getParameter("movieId"));
        if (movieId < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        User user = currentUser(request);
        try {
            if ("delete".equals(request.getParameter("action"))) {
                deleteReview(request, user);
            } else {
                submitReview(request, user);
            }
            response.sendRedirect(redirectUrl(request, movieId));
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void submitReview(HttpServletRequest request, User user) throws SQLException {
        ReviewForm form = readForm(request);
        try {
            boolean created = reviewService.submit(form, user.getUserId());
            ActivityLogger.info(request, LogEventType.BOOKING,
                    created ? "Gửi đánh giá phim" : "Cập nhật đánh giá phim", "Movie", form.getMovieId(),
                    "Rating=" + form.getRating());
            request.getSession().setAttribute(FLASH_SUCCESS,
                    created ? Messages.REVIEW_CREATED : Messages.REVIEW_UPDATED);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR,
                    e.getErrors().getOrDefault("general", e.getErrors().values().iterator().next()));
        }
    }

    private void deleteReview(HttpServletRequest request, User user) throws SQLException {
        int reviewId = parseInt(request.getParameter("reviewId"));
        try {
            reviewService.delete(reviewId, user.getUserId());
            ActivityLogger.info(request, LogEventType.BOOKING, "Xóa đánh giá phim", "Review", reviewId, null);
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.REVIEW_DELETED);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR,
                    e.getErrors().getOrDefault("general", Messages.CUSTOMER_ACTION_FAILED));
        }
    }

    // Phim vẫn còn công khai: quay về khung đánh giá trên trang chi tiết phim, tránh trùng giao diện
    private String redirectUrl(HttpServletRequest request, int movieId) throws SQLException {
        Movie movie = movieService.findById(movieId);
        String ctx = request.getContextPath();
        if (movie != null && MovieStatus.PUBLIC.contains(movie.getStatus())) {
            return ctx + "/movies/detail?id=" + movieId + "#reviews";
        }
        return ctx + "/customer/reviews?movieId=" + movieId;
    }

    private void render(HttpServletRequest request, HttpServletResponse response, Movie movie)
            throws SQLException, ServletException, IOException {
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        int userId = currentUser(request).getUserId();
        request.setAttribute("movie", movie);
        request.setAttribute("reviewSummary", reviewService.summarize(movie.getMovieId()));
        request.setAttribute("myReview", reviewService.findMine(movie.getMovieId(), userId));
        request.setAttribute("reviewPage",
                reviewService.listOthers(movie.getMovieId(), userId, parsePage(request)));
        request.setAttribute("canReview", reviewService.canWriteReview(movie.getMovieId(), userId));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private ReviewForm readForm(HttpServletRequest request) {
        ReviewForm form = new ReviewForm();
        form.setMovieId(parseInt(request.getParameter("movieId")));
        form.setRating(parseInt(request.getParameter("rating")));
        form.setComment(request.getParameter("comment"));
        return form;
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        request.getSession().setAttribute(FLASH_ERROR, message);
        response.sendRedirect(request.getContextPath() + "/customer/bookings");
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getSession(false).getAttribute("user");
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi xử lý đánh giá phim", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parsePage(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
