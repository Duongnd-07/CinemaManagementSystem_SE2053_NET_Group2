package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.ShowtimeService;
import vn.edu.fpt.util.Messages;

// UC-06: View Movie Details - /movies/detail?id=... cho Guest/Customer
@WebServlet("/movies/detail")
public class MovieDetailServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieDetailServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/movie-detail.jsp";
    private static final String FLASH_ERROR = "flashError";

    private final MovieService movieService = new MovieService();
    private final ShowtimeService showtimeService = new ShowtimeService();

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
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải chi tiết phim", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
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
