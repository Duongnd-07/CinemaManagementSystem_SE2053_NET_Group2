package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.AgeRating;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.service.MovieForm;
import vn.edu.fpt.service.MovieService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;
import vn.edu.fpt.util.PosterStorage;

// UC-23: Manage Movies
// Không đặt maxFileSize: file 5-10MB vẫn tới được PosterStorage.validate để báo lỗi ngay dưới ô nhập
@WebServlet("/manager/movies")
@MultipartConfig(maxRequestSize = MovieManagementServlet.MAX_REQUEST_SIZE)
public class MovieManagementServlet extends HttpServlet {
    static final int MAX_REQUEST_SIZE = 10 * 1024 * 1024;
    private static final Logger LOGGER = Logger.getLogger(MovieManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/movies.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final MovieService movieService = new MovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            MovieForm form = null;
            String edit = request.getParameter("edit");
            if (edit != null) {
                Movie movie = movieService.findById(parseInt(edit));
                if (movie == null) {
                    flash(request, FLASH_ERROR, Messages.MOVIE_NOT_FOUND);
                } else {
                    form = movieService.toForm(movie);
                }
            } else if ("1".equals(request.getParameter("add"))) {
                form = defaultForm();
            }
            render(request, response, form, new LinkedHashMap<>());
        } catch (SQLException e) {
            handleSystemError(request, response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_REQUEST_SIZE) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, Messages.MOVIE_POSTER_FILE_TOO_LARGE);
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            if ("archive".equals(request.getParameter("action"))) {
                archive(request, response);
            } else {
                save(request, response);
            }
        } catch (SQLException e) {
            handleSystemError(request, response, e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        MovieForm form = readForm(request);

        Part poster = request.getPart("posterFile");
        if (PosterStorage.hasFile(poster)) {
            String uploadError = PosterStorage.validate(poster);
            if (uploadError == null) {
                uploadError = storePoster(poster, form);
            }
            if (uploadError != null) {
                // Vẫn hiển thị lỗi của các trường khác cùng lúc với lỗi file
                Map<String, String> errors = new LinkedHashMap<>(movieService.validate(form));
                errors.put("poster", uploadError);
                render(request, response, form, errors);
                return;
            }
        }

        try {
            movieService.save(form);
        } catch (ValidationException e) {
            discardUploadedPoster(form);
            render(request, response, form, e.getErrors());
            return;
        } catch (SQLException | RuntimeException e) {
            discardUploadedPoster(form);
            throw e;
        }
        flash(request, FLASH_SUCCESS, Messages.MOVIE_SAVED);
        response.sendRedirect(request.getContextPath() + "/manager/movies" + listQuery(request));
    }

    // Trả về thông báo lỗi nếu không ghi được file, null nếu thành công
    private String storePoster(Part poster, MovieForm form) {
        try {
            form.setUploadedPoster(PosterStorage.store(poster));
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Không lưu được file poster", e);
            return Messages.MOVIE_POSTER_SAVE_FAILED;
        }
    }

    private void discardUploadedPoster(MovieForm form) {
        PosterStorage.deleteQuietly(form.getUploadedPoster());
        form.setUploadedPoster("");
    }

    private void archive(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        try {
            movieService.archive(parseInt(request.getParameter("movieId")));
            flash(request, FLASH_SUCCESS, Messages.MOVIE_ARCHIVED);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, e.getErrors().values().iterator().next());
        }
        response.sendRedirect(request.getContextPath() + "/manager/movies" + listQuery(request));
    }

    private void render(HttpServletRequest request, HttpServletResponse response, MovieForm form,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        String keyword = trim(request.getParameter("q"));
        String status = trim(request.getParameter("status"));
        String sort = trim(request.getParameter("sort"));
        Page<Movie> page = movieService.list(keyword, status, sort, parseInt(request.getParameter("page")));
        Map<String, Integer> statusCounts = movieService.countByStatus(keyword);

        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);
        request.setAttribute("moviePage", page);
        request.setAttribute("statusCounts", statusCounts);
        request.setAttribute("allCount", statusCounts.values().stream().mapToInt(Integer::intValue).sum());
        request.setAttribute("keyword", keyword);
        request.setAttribute("statusFilter", MovieStatus.isValid(status) ? status : "");
        request.setAttribute("sort", sort);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("genres", movieService.listGenres());
        request.setAttribute("ageRatings", AgeRating.ALL);
        request.setAttribute("statuses", MovieStatus.ALL);
        request.setAttribute("formats", MovieService.FORMATS);
        request.setAttribute("maxDuration", MovieService.MAX_DURATION);
        request.setAttribute("maxSynopsis", MovieService.MAX_SYNOPSIS);
        request.setAttribute("maxKeyword", MovieService.MAX_KEYWORD);
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "movies");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (form != null) {
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.setAttribute("showForm", true);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private MovieForm readForm(HttpServletRequest request) {
        MovieForm form = new MovieForm();
        form.setMovieId(parseInt(request.getParameter("movieId")));
        form.setTitle(trim(request.getParameter("title")));
        form.setSynopsis(trim(request.getParameter("synopsis")));
        form.setDirector(trim(request.getParameter("director")));
        form.setCast(trim(request.getParameter("cast")));
        form.setDuration(trim(request.getParameter("duration")));
        form.setAgeRating(trim(request.getParameter("ageRating")));
        form.setReleaseDate(trim(request.getParameter("releaseDate")));
        form.setPosterUrl(trim(request.getParameter("posterUrl")));
        form.setTrailerUrl(trim(request.getParameter("trailerUrl")));
        form.setStatus(trim(request.getParameter("movieStatus")));
        // Bỏ giá trị gửi trùng (checkbox bị lặp) để không lưu "2D,2D" hay chèn trùng Movie_Genre
        Set<String> formats = new LinkedHashSet<>();
        String[] formatValues = request.getParameterValues("formats");
        if (formatValues != null) {
            for (String format : formatValues) {
                formats.add(trim(format));
            }
        }
        form.setFormats(new ArrayList<>(formats));
        Set<Integer> genreIds = new LinkedHashSet<>();
        String[] genres = request.getParameterValues("genreIds");
        if (genres != null) {
            for (String genre : genres) {
                genreIds.add(parseInt(genre));
            }
        }
        form.setGenreIds(new ArrayList<>(genreIds));
        return form;
    }

    private MovieForm defaultForm() {
        MovieForm form = new MovieForm();
        form.setStatus(MovieStatus.COMING_SOON);
        return form;
    }

    // Giữ bộ lọc/trang hiện tại khi quay lại danh sách sau thao tác
    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("q", "status", "sort", "page")) {
            String value = trim(request.getParameter(name));
            if (!value.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    private void handleSystemError(HttpServletRequest request, HttpServletResponse response, SQLException e)
            throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu phim", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
