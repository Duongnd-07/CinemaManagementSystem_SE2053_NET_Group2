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
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.ReviewService;
import vn.edu.fpt.service.ShowtimeService;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-06: View Movie Details - /movies/detail?id=... cho Guest/Customer
@WebServlet("/movies/detail")
public class MovieDetailServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieDetailServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/movie-detail.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final MovieService movieService = new MovieService();
    private final ShowtimeService showtimeService = new ShowtimeService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Movie movie = movieService.findPublicMovie(parseInt(request.getParameter("id")));
            if (movie == null) {
                // E1: phim không tồn tại, đã ẩn hoặc đã kết thúc -> quay về danh sách phim kèm thông báo
                request.getSession().setAttribute(FLASH_ERROR, Messages.MOVIE_DETAIL_UNAVAILABLE);
                response.sendRedirect(request.getContextPath() + "/movies");
                return;
            }
            request.setAttribute("movie", movie);
            request.setAttribute("trailerId", movieService.youtubeVideoId(movie.getTrailerUrl()));
            request.setAttribute("canBook", showtimeService.hasBookableShowtimes(movie.getMovieId()));
            request.setAttribute("activeNav",
                    MovieStatus.COMING_SOON.equals(movie.getStatus()) ? "coming-soon" : "now-showing");
            setReviewAttributes(request, movie);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải chi tiết phim", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    // UC-17: khung đánh giá ngay trên trang chi tiết phim; review của khách được đặt trong canReview/myReview
    private void setReviewAttributes(HttpServletRequest request, Movie movie) throws SQLException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        boolean isCustomer = user != null && Role.CUSTOMER.equals(user.getRole());
        int userId = isCustomer ? user.getUserId() : 0;

        if (session != null) {
            for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
                Object value = session.getAttribute(key);
                if (value != null) {
                    request.setAttribute(key, value);
                    session.removeAttribute(key);
                }
            }
        }

        request.setAttribute("reviewSummary", reviewService.summarize(movie.getMovieId()));
        request.setAttribute("reviewPage", reviewService.listOthers(movie.getMovieId(), userId, parsePage(request)));
        request.setAttribute("isCustomer", isCustomer);
        if (isCustomer) {
            request.setAttribute("myReview", reviewService.findMine(movie.getMovieId(), userId));
            request.setAttribute("canReview", reviewService.canWriteReview(movie.getMovieId(), userId));
            request.setAttribute("csrfToken", CsrfToken.get(session));
        }
    }

    // "page" khớp với tham số mà ui:pagination sinh ra trong URL
    private int parsePage(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
