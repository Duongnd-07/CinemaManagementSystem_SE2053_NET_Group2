package vn.edu.fpt.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
    public static final String POSTER_DIR = PosterStorage.POSTER_DIR;
    public static final List<String> FORMATS = List.of("2D", "3D", "IMAX");

    private static final int MAX_TITLE = 200;
    private static final int MAX_DIRECTOR = 100;
    private static final int MAX_CAST = 500;
    private static final int MAX_URL = 500;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    private static final Pattern YOUTUBE = Pattern.compile(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com/(watch\\?(.*&)?v=|embed/|shorts/)|youtu\\.be/)[\\w-]{6,}.*$",
            Pattern.CASE_INSENSITIVE);

    private final MovieDAO movieDAO = new MovieDAO();
    private final GenreDAO genreDAO = new GenreDAO();

    public Page<Movie> list(String keyword, String status, String sort, int page) throws SQLException {
        String kw = keyword == null ? "" : keyword.trim();
        String statusFilter = MovieStatus.isValid(status) ? status : null;
        String orderBy = orderBy(sort);
        return Page.of(page, PAGE_SIZE, movieDAO.count(kw, statusFilter),
                (offset, limit) -> movieDAO.search(kw, statusFilter, orderBy, offset, limit));
    }

    // Số phim theo từng trạng thái (hiển thị ở các tab lọc), tính theo từ khóa tìm kiếm
    public Map<String, Integer> countByStatus(String keyword) throws SQLException {
        return movieDAO.countByStatus(keyword == null ? "" : keyword.trim());
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
        Map<String, String> errors = new LinkedHashMap<>();

        String title = form.getTitle().trim();
        if (title.isEmpty()) {
            errors.put("title", Messages.format(Messages.MSG02, "Tên phim"));
        } else if (title.length() > MAX_TITLE) {
            errors.put("title", Messages.format(Messages.MSG08, MAX_TITLE));
        } else if (movieDAO.existsByTitle(title, form.getMovieId())) {
            // BR-MOV-01
            errors.put("title", Messages.MOVIE_TITLE_DUPLICATE);
        }

        if (form.getDirector().trim().length() > MAX_DIRECTOR) {
            errors.put("director", Messages.format(Messages.MSG08, MAX_DIRECTOR));
        }
        if (form.getCast().trim().length() > MAX_CAST) {
            errors.put("cast", Messages.format(Messages.MSG08, MAX_CAST));
        }

        // BR-MOV-02
        if (form.getDuration().trim().isEmpty()) {
            errors.put("duration", Messages.format(Messages.MSG02, "Thời lượng"));
        } else if (parseDuration(form.getDuration()) <= 0) {
            errors.put("duration", Messages.MOVIE_DURATION_INVALID);
        }
        if (form.getAgeRating().isEmpty()) {
            errors.put("ageRating", Messages.format(Messages.MSG02, "Phân loại tuổi"));
        } else if (!AgeRating.isValid(form.getAgeRating())) {
            errors.put("ageRating", Messages.MOVIE_AGE_RATING_INVALID);
        }

        if (form.getReleaseDate().trim().isEmpty()) {
            errors.put("releaseDate", Messages.format(Messages.MSG02, "Ngày khởi chiếu"));
        } else if (parseDate(form.getReleaseDate()) == null) {
            errors.put("releaseDate", Messages.MOVIE_DATE_INVALID);
        }

        if (form.getStatus().isEmpty()) {
            errors.put("status", Messages.format(Messages.MSG02, "Trạng thái"));
        } else if (!MovieStatus.isValid(form.getStatus())) {
            errors.put("status", Messages.MOVIE_STATUS_INVALID);
        } else if (MovieStatus.HIDDEN.equals(form.getStatus()) && form.getMovieId() > 0) {
            checkCanHide(form.getMovieId(), errors);
        }

        String poster = form.getPosterUrl().trim();
        if (poster.length() > MAX_URL) {
            errors.put("poster", Messages.format(Messages.MSG08, MAX_URL));
        } else if (!poster.isEmpty() && !isValidPoster(poster)) {
            errors.put("poster", Messages.MOVIE_POSTER_URL_INVALID);
        }
        String trailer = form.getTrailerUrl().trim();
        if (trailer.length() > MAX_URL) {
            errors.put("trailer", Messages.format(Messages.MSG08, MAX_URL));
        } else if (!trailer.isEmpty() && !YOUTUBE.matcher(trailer).matches()) {
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

    public int save(MovieForm form) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Movie movie = toMovie(form);
        boolean isNew = form.getMovieId() <= 0;
        if (!isNew && movieDAO.findById(form.getMovieId()) == null) {
            throw new ValidationException(Map.of("general", Messages.MOVIE_NOT_FOUND));
        }

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int movieId;
                if (isNew) {
                    movieId = movieDAO.insert(conn, movie);
                } else {
                    movieDAO.update(conn, movie);
                    movieId = movie.getMovieId();
                }
                movieDAO.replaceGenres(conn, movieId, form.getGenreIds());
                conn.commit();
                return movieId;
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                    throw new ValidationException(Map.of("title", Messages.MOVIE_TITLE_DUPLICATE));
                }
                throw e;
            }
        }
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
        Map<String, String> errors = new LinkedHashMap<>();
        checkCanHide(movieId, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(Map.of("general", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES));
        }
        movieDAO.updateStatus(movieId, MovieStatus.HIDDEN);
    }

    // BR-MOV-04
    private void checkCanHide(int movieId, Map<String, String> errors) throws SQLException {
        Movie current = movieDAO.findById(movieId);
        boolean alreadyHidden = current != null && MovieStatus.HIDDEN.equals(current.getStatus());
        if (!alreadyHidden && movieDAO.hasActiveBookedShowtimes(movieId)) {
            errors.put("status", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES);
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
        movie.setPosterUrl(blankToNull(form.getPosterUrl()));
        movie.setTrailerUrl(blankToNull(form.getTrailerUrl()));
        movie.setFormats(form.getFormats().isEmpty() ? null : String.join(",", form.getFormats()));
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

    private boolean isValidPoster(String poster) {
        if (poster.startsWith(POSTER_DIR)) {
            return !poster.contains("..");
        }
        try {
            URI uri = new URI(poster);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
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
