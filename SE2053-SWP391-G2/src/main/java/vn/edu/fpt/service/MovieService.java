package vn.edu.fpt.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import vn.edu.fpt.dao.GenreDAO;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.model.AgeRating;
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;
import vn.edu.fpt.util.PosterStorage;

public class MovieService {
    public static final int PAGE_SIZE = 8;
    public static final int MAX_DURATION = 600;
    public static final int MAX_SYNOPSIS = 4000;
    public static final int MAX_KEYWORD = 100;
    public static final List<String> FORMATS = List.of("2D", "3D", "IMAX");

    private static final int MAX_TITLE = 200;
    private static final int MAX_DIRECTOR = 100;
    private static final int MAX_CAST = 500;
    private static final int MAX_URL = 500;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1900, 1, 1);
    private static final int MAX_RELEASE_YEARS_AHEAD = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    // PosterURL/TrailerURL là cột VARCHAR: chỉ nhận ký tự ASCII in được, không khoảng trắng
    private static final Pattern URL_CHARS = Pattern.compile("^[\\x21-\\x7E]+$");
    private static final Pattern YOUTUBE = Pattern.compile(
            "^https?://(www\\.|m\\.)?(youtube\\.com/(watch\\?(.*&)?v=|embed/|shorts/)|youtu\\.be/)[\\w-]{11}([?&#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private final MovieDAO movieDAO = new MovieDAO();
    private final GenreDAO genreDAO = new GenreDAO();

    public Page<Movie> list(String keyword, String status, String sort, int page) throws SQLException {
        String kw = normalizeKeyword(keyword);
        String statusFilter = MovieStatus.isValid(status) ? status : null;
        String orderBy = orderBy(sort);
        return Page.of(page, PAGE_SIZE, movieDAO.count(kw, statusFilter),
                (offset, limit) -> movieDAO.search(kw, statusFilter, orderBy, offset, limit));
    }

    // Số phim theo từng trạng thái (hiển thị ở các tab lọc), tính theo từ khóa tìm kiếm
    public Map<String, Integer> countByStatus(String keyword) throws SQLException {
        return movieDAO.countByStatus(normalizeKeyword(keyword));
    }

    public Movie findById(int movieId) throws SQLException {
        return movieDAO.findById(movieId);
    }

    public List<Genre> listGenres() throws SQLException {
        return genreDAO.findAll();
    }

    public MovieForm toForm(Movie movie) {
        MovieForm form = new MovieForm();
        form.setMovieId(movie.getMovieId());
        form.setTitle(nullToEmpty(movie.getTitle()));
        form.setSynopsis(nullToEmpty(movie.getSynopsis()));
        form.setDirector(nullToEmpty(movie.getDirector()));
        form.setCast(nullToEmpty(movie.getCast()));
        form.setDuration(String.valueOf(movie.getDurationMinutes()));
        form.setAgeRating(nullToEmpty(movie.getAgeRating()));
        form.setReleaseDate(movie.getReleaseDate() == null ? "" : movie.getReleaseDate().toString());
        form.setPosterUrl(nullToEmpty(movie.getPosterUrl()));
        form.setTrailerUrl(nullToEmpty(movie.getTrailerUrl()));
        form.setStatus(nullToEmpty(movie.getStatus()));
        if (movie.getFormats() != null && !movie.getFormats().isBlank()) {
            form.setFormats(List.of(movie.getFormats().split(",")));
        }
        for (Genre genre : movie.getGenres()) {
            form.getGenreIds().add(genre.getGenreId());
        }
        return form;
    }

    // E1: trả về toàn bộ lỗi theo từng trường; map rỗng nghĩa là hợp lệ
    public Map<String, String> validate(MovieForm form) throws SQLException {
        Movie current = null;
        if (form.getMovieId() > 0) {
            current = movieDAO.findById(form.getMovieId());
            if (current == null) {
                return Map.of("general", Messages.MOVIE_NOT_FOUND);
            }
        }
        return validate(form, current);
    }

    public int save(MovieForm form) throws SQLException, ValidationException {
        Movie current = null;
        if (form.getMovieId() > 0) {
            current = movieDAO.findById(form.getMovieId());
            if (current == null) {
                throw new ValidationException(Map.of("general", Messages.MOVIE_NOT_FOUND));
            }
        }
        Map<String, String> errors = validate(form, current);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Movie movie = toMovie(form);
        int movieId = persist(movie, form.getGenreIds());
        // Poster cũ do hệ thống lưu không còn được dùng (đã thay/xóa) thì dọn file; URL ngoài được bỏ qua
        if (current != null && !Objects.equals(current.getPosterUrl(), movie.getPosterUrl())) {
            PosterStorage.deleteQuietly(current.getPosterUrl());
        }
        return movieId;
    }

    // GB-12: không xóa cứng phim, chỉ chuyển trạng thái Hidden
    public void archive(int movieId) throws SQLException, ValidationException {
        Movie movie = movieDAO.findById(movieId);
        if (movie == null) {
            throw new ValidationException(Map.of("general", Messages.MOVIE_NOT_FOUND));
        }
        if (MovieStatus.HIDDEN.equals(movie.getStatus())) {
            return;
        }
        // BR-MOV-04
        if (movieDAO.hasActiveBookedShowtimes(movieId)) {
            throw new ValidationException(Map.of("general", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES));
        }
        movieDAO.updateStatus(movieId, MovieStatus.HIDDEN);
    }

    private Map<String, String> validate(MovieForm form, Movie current) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        int movieId = current == null ? 0 : current.getMovieId();

        String title = form.getTitle().trim();
        if (title.isEmpty()) {
            errors.put("title", Messages.format(Messages.MSG02, "Tên phim"));
        } else if (title.length() > MAX_TITLE) {
            errors.put("title", Messages.format(Messages.MSG08, MAX_TITLE));
        } else if (movieDAO.existsByTitle(title, movieId)) {
            // BR-MOV-01
            errors.put("title", Messages.MOVIE_TITLE_DUPLICATE);
        }

        if (form.getSynopsis().trim().length() > MAX_SYNOPSIS) {
            errors.put("synopsis", Messages.format(Messages.MSG08, String.valueOf(MAX_SYNOPSIS)));
        }
        if (form.getDirector().trim().length() > MAX_DIRECTOR) {
            errors.put("director", Messages.format(Messages.MSG08, MAX_DIRECTOR));
        }
        if (form.getCast().trim().length() > MAX_CAST) {
            errors.put("cast", Messages.format(Messages.MSG08, MAX_CAST));
        }

        validateDuration(form, current, errors);

        // GB-03
        if (form.getAgeRating().isEmpty()) {
            errors.put("ageRating", Messages.format(Messages.MSG02, "Phân loại tuổi"));
        } else if (!AgeRating.isValid(form.getAgeRating())) {
            errors.put("ageRating", Messages.MOVIE_AGE_RATING_INVALID);
        }

        LocalDate releaseDate = validateReleaseDate(form, errors);
        validateStatus(form, current, releaseDate, errors);
        validatePoster(form, current, errors);

        String trailer = form.getTrailerUrl().trim();
        if (trailer.length() > MAX_URL) {
            errors.put("trailer", Messages.format(Messages.MSG08, MAX_URL));
        } else if (!trailer.isEmpty() && !(URL_CHARS.matcher(trailer).matches() && YOUTUBE.matcher(trailer).matches())) {
            errors.put("trailer", Messages.MOVIE_TRAILER_URL_INVALID);
        }

        for (String format : form.getFormats()) {
            if (!FORMATS.contains(format)) {
                errors.put("formats", Messages.MOVIE_FORMAT_INVALID);
                break;
            }
        }
        if (!form.getGenreIds().isEmpty() && genreDAO.countExisting(form.getGenreIds()) != form.getGenreIds().size()) {
            errors.put("genres", Messages.MOVIE_GENRE_INVALID);
        }
        return errors;
    }

    // BR-MOV-02
    private void validateDuration(MovieForm form, Movie current, Map<String, String> errors) throws SQLException {
        if (form.getDuration().trim().isEmpty()) {
            errors.put("duration", Messages.format(Messages.MSG02, "Thời lượng"));
            return;
        }
        int duration = parseDuration(form.getDuration());
        if (duration < 1 || duration > MAX_DURATION) {
            errors.put("duration", Messages.format(Messages.MOVIE_DURATION_INVALID, MAX_DURATION));
        } else if (current != null && duration != current.getDurationMinutes()
                && movieDAO.hasUnfinishedShowtimes(current.getMovieId())) {
            errors.put("duration", Messages.MOVIE_DURATION_LOCKED);
        }
    }

    // Trả về ngày khởi chiếu hợp lệ, hoặc null nếu có lỗi
    private LocalDate validateReleaseDate(MovieForm form, Map<String, String> errors) {
        if (form.getReleaseDate().trim().isEmpty()) {
            errors.put("releaseDate", Messages.format(Messages.MSG02, "Ngày khởi chiếu"));
            return null;
        }
        LocalDate releaseDate = parseDate(form.getReleaseDate());
        if (releaseDate == null) {
            errors.put("releaseDate", Messages.MOVIE_DATE_INVALID);
            return null;
        }
        LocalDate maxDate = LocalDate.now().plusYears(MAX_RELEASE_YEARS_AHEAD);
        if (releaseDate.isBefore(MIN_RELEASE_DATE) || releaseDate.isAfter(maxDate)) {
            errors.put("releaseDate", Messages.format(Messages.MOVIE_DATE_OUT_OF_RANGE,
                    MIN_RELEASE_DATE.format(DATE_FORMAT), maxDate.format(DATE_FORMAT)));
            return null;
        }
        return releaseDate;
    }

    private void validateStatus(MovieForm form, Movie current, LocalDate releaseDate, Map<String, String> errors)
            throws SQLException {
        String status = form.getStatus();
        if (status.isEmpty()) {
            errors.put("status", Messages.format(Messages.MSG02, "Trạng thái"));
            return;
        }
        if (!MovieStatus.isValid(status)) {
            errors.put("status", Messages.MOVIE_STATUS_INVALID);
            return;
        }
        // Trạng thái chiếu phải khớp ngày khởi chiếu; Hidden không phụ thuộc ngày
        if (releaseDate != null) {
            LocalDate today = LocalDate.now();
            if (MovieStatus.COMING_SOON.equals(status) && releaseDate.isBefore(today)) {
                errors.put("status", Messages.MOVIE_COMING_SOON_DATE_PASSED);
                return;
            }
            boolean released = MovieStatus.NOW_SHOWING.equals(status) || MovieStatus.ENDED.equals(status);
            if (released && releaseDate.isAfter(today)) {
                errors.put("status", Messages.MOVIE_NOT_RELEASED_YET);
                return;
            }
        }
        // BR-MOV-04: không ẩn/kết thúc phim khi còn suất chiếu chưa diễn ra đã bán vé
        boolean closing = MovieStatus.HIDDEN.equals(status) || MovieStatus.ENDED.equals(status);
        if (closing && current != null && !status.equals(current.getStatus())
                && movieDAO.hasActiveBookedShowtimes(current.getMovieId())) {
            errors.put("status", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES);
        }
    }

    private void validatePoster(MovieForm form, Movie current, Map<String, String> errors) {
        // Có file upload thì URL nhập tay bị thay thế, không cần kiểm tra
        if (!form.getUploadedPoster().isEmpty()) {
            return;
        }
        String poster = form.getPosterUrl().trim();
        if (poster.length() > MAX_URL) {
            errors.put("poster", Messages.format(Messages.MSG08, MAX_URL));
        } else if (!poster.isEmpty() && !isValidPoster(poster, current)) {
            errors.put("poster", Messages.MOVIE_POSTER_URL_INVALID);
        }
    }

    // Đường dẫn file nội bộ chỉ được giữ nguyên của chính phim đó, tránh trỏ sang (rồi xóa nhầm) poster phim khác
    private boolean isValidPoster(String poster, Movie current) {
        if (PosterStorage.isStoredPath(poster)) {
            return current != null && poster.equals(current.getPosterUrl());
        }
        if (!URL_CHARS.matcher(poster).matches()) {
            return false;
        }
        try {
            URI uri = new URI(poster);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private int persist(Movie movie, List<Integer> genreIds) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int movieId;
                if (movie.getMovieId() > 0) {
                    movieDAO.update(conn, movie);
                    movieId = movie.getMovieId();
                } else {
                    movieId = movieDAO.insert(conn, movie);
                }
                movieDAO.replaceGenres(conn, movieId, genreIds);
                conn.commit();
                return movieId;
            } catch (SQLException e) {
                conn.rollback();
                // Hai người lưu cùng tên đồng thời: ràng buộc UNIQUE(Title) chặn ở DB
                if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                    throw new ValidationException(Map.of("title", Messages.MOVIE_TITLE_DUPLICATE));
                }
                throw e;
            }
        }
    }

    private Movie toMovie(MovieForm form) {
        Movie movie = new Movie();
        movie.setMovieId(form.getMovieId());
        movie.setTitle(form.getTitle().trim());
        movie.setSynopsis(blankToNull(form.getSynopsis()));
        movie.setDirector(blankToNull(form.getDirector()));
        movie.setCast(blankToNull(form.getCast()));
        movie.setDurationMinutes(parseDuration(form.getDuration()));
        movie.setAgeRating(form.getAgeRating());
        movie.setReleaseDate(parseDate(form.getReleaseDate()));
        movie.setPosterUrl(form.getUploadedPoster().isEmpty()
                ? blankToNull(form.getPosterUrl()) : form.getUploadedPoster());
        movie.setTrailerUrl(blankToNull(form.getTrailerUrl()));
        // Lưu theo thứ tự cố định của FORMATS để dữ liệu nhất quán
        String formats = FORMATS.stream().filter(form.getFormats()::contains).collect(Collectors.joining(","));
        movie.setFormats(formats.isEmpty() ? null : formats);
        movie.setStatus(form.getStatus());
        return movie;
    }

    private String orderBy(String sort) {
        if ("name".equals(sort)) {
            return "Title ASC, MovieID DESC";
        }
        if ("release".equals(sort)) {
            return "ReleaseDate DESC, MovieID DESC";
        }
        return "MovieID DESC";
    }

    private String normalizeKeyword(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        return kw.length() > MAX_KEYWORD ? kw.substring(0, MAX_KEYWORD) : kw;
    }

    private int parseDuration(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
