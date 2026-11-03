package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.AgeRating;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.service.MovieSearchForm;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.util.Messages;

// UC-04: View Movie List - trang chủ ("") và mục Phim (/movies) cho Guest/Customer
// UC-05: Search & Filter Movies - tìm kiếm/lọc trên mục Phim qua tham số q, genre, director, cast, age
@WebServlet({"", "/movies"})
public class MovieListServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(MovieListServlet.class.getName());
    private static final String HOME_VIEW = "/WEB-INF/views/customer/home.jsp";
    private static final String MOVIES_VIEW = "/WEB-INF/views/customer/movies.jsp";
    private static final String TAB_NOW_SHOWING = "now-showing";
    private static final String TAB_COMING_SOON = "coming-soon";

    private final MovieService movieService = new MovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/movies".equals(request.getServletPath())) {
                showMovies(request, response);
            } else {
                showHome(request, response);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải danh sách phim công khai", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void showHome(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Map<String, List<Movie>> groups = movieService.listPublicMovies();
        request.setAttribute("nowShowing", groups.get(MovieStatus.NOW_SHOWING));
        request.setAttribute("comingSoon", groups.get(MovieStatus.COMING_SOON));
        request.setAttribute("emptyMessage", Messages.MOVIE_LIST_EMPTY);
        request.setAttribute("activeNav", "home");
        request.getRequestDispatcher(HOME_VIEW).forward(request, response);
    }

    private void showMovies(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        MovieSearchForm search = new MovieSearchForm();
        search.setKeyword(param(request, "q"));
        search.setGenreId(param(request, "genre"));
        search.setDirector(param(request, "director"));
        search.setCast(param(request, "cast"));
        search.setAgeRating(param(request, "age"));
        Map<String, List<Movie>> groups = movieService.searchPublicMovies(search);
        List<Movie> nowShowing = groups.get(MovieStatus.NOW_SHOWING);
        List<Movie> comingSoon = groups.get(MovieStatus.COMING_SOON);

        String tab = param(request, "tab");
        if (!TAB_NOW_SHOWING.equals(tab) && !TAB_COMING_SOON.equals(tab)) {
            // Không chọn tab: mặc định Đang chiếu, trừ khi kết quả tìm kiếm chỉ có ở Sắp chiếu
            tab = nowShowing.isEmpty() && !comingSoon.isEmpty() ? TAB_COMING_SOON : TAB_NOW_SHOWING;
        }

        request.setAttribute("nowShowing", nowShowing);
        request.setAttribute("comingSoon", comingSoon);
        request.setAttribute("search", search);
        request.setAttribute("filterQuery", toQuery(search));
        request.setAttribute("filtered", !search.isEmpty());
        request.setAttribute("genres", movieService.listGenres());
        request.setAttribute("ageRatings", AgeRating.ALL);
        request.setAttribute("emptyMessage",
                search.isEmpty() ? Messages.MOVIE_LIST_EMPTY : Messages.MOVIE_SEARCH_NO_RESULT);
        request.setAttribute("tab", tab);
        request.setAttribute("activeNav", tab);
        request.getRequestDispatcher(MOVIES_VIEW).forward(request, response);
    }

    // Chuỗi tham số bộ lọc (đã URL-encode, kết thúc bằng &) để link tab giữ nguyên bộ lọc;
    // không dùng <c:url> vì nó gắn ;jsessionid vào URL khi trình duyệt chưa có cookie phiên
    private String toQuery(MovieSearchForm search) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", search.getKeyword());
        params.put("genre", search.getGenreId());
        params.put("director", search.getDirector());
        params.put("cast", search.getCast());
        params.put("age", search.getAgeRating());
        StringBuilder query = new StringBuilder();
        params.forEach((name, value) -> {
            if (!value.isEmpty()) {
                query.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append('&');
            }
        });
        return query.toString();
    }

    private String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value;
    }
}
