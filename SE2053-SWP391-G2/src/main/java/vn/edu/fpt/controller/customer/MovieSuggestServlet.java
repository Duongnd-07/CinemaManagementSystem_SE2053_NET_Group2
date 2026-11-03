package vn.edu.fpt.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.service.MovieService;

// UC-05: gợi ý phim khi gõ tên vào ô tìm kiếm - trả JSON cho assets/js/movie-suggest.js
@WebServlet("/movies/suggest")
public class MovieSuggestServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieSuggestServlet.class.getName());

    private final MovieService movieService = new MovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Movie> movies;
        try {
            movies = movieService.suggestPublicMovies(request.getParameter("q"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải gợi ý phim", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        String ctx = request.getContextPath();
        StringBuilder json = new StringBuilder("[");
        for (Movie movie : movies) {
            if (json.length() > 1) {
                json.append(',');
            }
            String poster = movie.getPosterUrl();
            if (poster != null && !poster.isEmpty() && !poster.startsWith("http")) {
                poster = ctx + "/" + poster;
            }
            json.append("{\"id\":").append(movie.getMovieId())
                    .append(",\"title\":").append(quote(movie.getTitle()))
                    .append(",\"ageRating\":").append(quote(movie.getAgeRating()))
                    .append(",\"status\":").append(quote(movie.getStatus()))
                    .append(",\"year\":").append(movie.getReleaseDate() == null ? "null" : movie.getReleaseDate().getYear())
                    .append(",\"poster\":").append(quote(poster))
                    .append('}');
        }
        json.append(']');
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(json.toString());
    }

    // Chuỗi JSON: thoát ", \, ký tự điều khiển và < > & để an toàn khi nhúng
    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20 || ch == '<' || ch == '>' || ch == '&') {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
