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

// UC-08: View Showtimes - /showtimes?movieId=...&date=yyyy-MM-dd&format=...&room=... cho Guest/Customer
@WebServlet("/showtimes")
public class ShowtimeListServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ShowtimeListServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/showtimes.jsp";
    private static final String FLASH_ERROR = "flashError";

    private final MovieService movieService = new MovieService();
    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Movie movie = movieService.findPublicMovie(parseInt(request.getParameter("movieId")));
            if (movie == null) {
                // Phim không còn công khai thì không xem lịch chiếu được (giống UC-06 E1)
                request.getSession().setAttribute(FLASH_ERROR, Messages.MOVIE_DETAIL_UNAVAILABLE);
                response.sendRedirect(request.getContextPath() + "/movies");
                return;
            }
            request.setAttribute("movie", movie);
            request.setAttribute("schedule", showtimeService.getSchedule(movie.getMovieId(),
                    request.getParameter("date"), request.getParameter("format"), request.getParameter("room"),
                    request.getParameter("translationType")));
            request.setAttribute("emptyMessage", Messages.SHOWTIME_NONE_FOR_DATE);
            request.setAttribute("activeNav",
                    MovieStatus.COMING_SOON.equals(movie.getStatus()) ? "coming-soon" : "now-showing");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải lịch chiếu", e);
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
