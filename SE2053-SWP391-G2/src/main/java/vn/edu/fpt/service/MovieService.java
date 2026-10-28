package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
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
    public static final int PUBLIC_PAGE_SIZE = 12;
    public static final int MAX_DURATION = 600;
    public static final int MAX_SYNOPSIS = 4000;
    public static final int MAX_KEYWORD = 100;
    public static final int SUGGEST_LIMIT = 8;
    public static final List<String> FORMATS = List.of("2D", "3D", "IMAX");

    private static final int MAX_TITLE = 200;
    private static final int MAX_DIRECTOR = 100;
    private static final int MAX_CAST = 500;
    private static final int MAX_COUNTRY = 100;
    private static final int MAX_LANGUAGE = 100;
    private static final int MAX_DISTRIBUTOR = 200;
    private static final int MAX_URL = 500;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1900, 1, 1);
    private static final int MAX_RELEASE_YEARS_AHEAD = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    // TrailerURL là cột VARCHAR: chỉ nhận ký tự ASCII in được, không khoảng trắng
    private static final Pattern URL_CHARS = Pattern.compile("^[\\x21-\\x7E]+$");
    private static final Pattern YOUTUBE = Pattern.compile(
            "^https?://(www\\.|m\\.)?(youtube\\.com/(watch\\?(.*&)?v=|embed/|shorts/)|youtu\\.be/)[\\w-]{11}([?&#].*)?$",
            Pattern.CASE_INSENSITIVE);
    // Lấy mã video từ các dạng link mà YOUTUBE chấp nhận
    private static final Pattern YOUTUBE_ID = Pattern.compile(
            "(?:[?&]v=|/embed/|/shorts/|youtu\\.be/)([\\w-]{11})", Pattern.CASE_INSENSITIVE);

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

    public List<Movie> listAllTitles() throws SQLException {
        return movieDAO.findAllTitles();
    }

    // UC-04: phim công khai nhóm theo trạng thái (Đang chiếu, Sắp chiếu); nhóm không có phim là danh sách rỗng
    public Map<String, List<Movie>> listPublicMovies() throws SQLException {
        return searchPublicMovies(new MovieSearchForm());
    }

    // UC-05: như UC-04 nhưng chỉ lấy phim khớp mọi tiêu chí của form (AND); form rỗng trả về toàn bộ
    public Map<String, List<Movie>> searchPublicMovies(MovieSearchForm form) throws SQLException {
        normalize(form);
        Integer genreId = form.getGenreId().isEmpty() ? null : Integer.valueOf(form.getGenreId());
        Map<String, List<Movie>> groups = new LinkedHashMap<>();
        for (String status : MovieStatus.PUBLIC) {
            groups.put(status, new ArrayList<>());
        }
        for (Movie movie : movieDAO.findByStatuses(MovieStatus.PUBLIC, form.getKeyword(), genreId,
                form.getDirector(), form.getCast(), allowedRatings(form.getAgeRating()))) {
            groups.get(publicStatusOf(movie)).add(movie);
        }
        return groups;
    }

    // UC-04: phân trang phim của tab đang xem; danh mục đã nạp đủ từ searchPublicMovies để đếm số phim ở cả hai tab
    public Page<Movie> pagePublicMovies(List<Movie> movies, int page) throws SQLException {
        return Page.of(page, PUBLIC_PAGE_SIZE, movies.size(),
                (offset, limit) -> movies.subList(offset, Math.min(offset + limit, movies.size())));
    }

    // Lọc theo độ tuổi người xem: chọn một nhãn thì gồm mọi phim người ở độ tuổi đó được xem
    // (AgeRating.ALL xếp từ P tới C18), vd C16 -> P, C13, C16. Rỗng = không lọc.
    private List<String> allowedRatings(String ageRating) {
        int index = AgeRating.ALL.indexOf(ageRating);
        return index < 0 ? List.of() : AgeRating.ALL.subList(0, index + 1);
    }

    // UC-06: phim để xem chi tiết; null nếu không tồn tại hoặc không còn công khai (Ended/Hidden) - E1
    public Movie findPublicMovie(int movieId) throws SQLException {
        Movie movie = movieId > 0 ? movieDAO.findById(movieId) : null;
        if (movie == null || matchPublicStatus(movie.getStatus()) == null) {
            return null;
        }
        movie.setStatus(matchPublicStatus(movie.getStatus()));
        return movie;
    }

    // UC-06: mã video YouTube (11 ký tự) để nhúng trình phát; null nếu không phải link YouTube hợp lệ
    public String youtubeVideoId(String trailerUrl) {
        String url = trailerUrl == null ? "" : trailerUrl.trim();
        if (url.isEmpty() || !URL_CHARS.matcher(url).matches() || !YOUTUBE.matcher(url).matches()) {
            return null;
        }
        Matcher matcher = YOUTUBE_ID.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    // SQL Server so khớp Status không phân biệt hoa thường và bỏ khoảng trắng cuối, nên DB có thể trả về
    // "now showing " cho điều kiện IN ('Now Showing'); quy về đúng hằng số để không làm hỏng việc nhóm phim
    private String publicStatusOf(Movie movie) {
        String status = matchPublicStatus(movie.getStatus());
        if (status == null) {
            throw new IllegalStateException("Trạng thái phim không thuộc nhóm công khai: " + movie.getStatus());
        }
        return status;
    }

    private String matchPublicStatus(String status) {
        String value = status == null ? "" : status.trim();
        for (String publicStatus : MovieStatus.PUBLIC) {
            if (publicStatus.equalsIgnoreCase(value)) {
                return publicStatus;
            }
        }
        return null;
    }

    // UC-05: gợi ý tên phim khi người dùng đang gõ; chỉ phim công khai
    public List<Movie> suggestPublicMovies(String keyword) throws SQLException {
        return movieDAO.suggestByTitle(MovieStatus.PUBLIC, normalizeKeyword(keyword), SUGGEST_LIMIT);
    }

    // Cắt khoảng trắng, giới hạn độ dài; thể loại/nhãn tuổi không hợp lệ thì bỏ qua bộ lọc đó
    private void normalize(MovieSearchForm form) {
        form.setKeyword(normalizeKeyword(form.getKeyword()));
        form.setDirector(normalizeKeyword(form.getDirector()));
        form.setCast(normalizeKeyword(form.getCast()));
        String rating = form.getAgeRating() == null ? "" : form.getAgeRating().trim();
        form.setAgeRating(AgeRating.isValid(rating) ? rating : "");
        String genre = form.getGenreId() == null ? "" : form.getGenreId().trim();
        form.setGenreId(parsePositiveInt(genre) > 0 ? genre : "");
    }

    private int parsePositiveInt(String value) {
        try {
            return Math.max(Integer.parseInt(value), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        form.setEndDate(movie.getEndDate() == null ? "" : movie.getEndDate().toString());
        form.setCountry(nullToEmpty(movie.getCountry()));
        form.setLanguage(nullToEmpty(movie.getLanguage()));
        form.setDistributor(nullToEmpty(movie.getDistributor()));
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
        if (movieDAO.hasUnfinishedShowtimes(movieId)) {
            throw new ValidationException(Map.of("general", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES));
        }
        movieDAO.updateStatus(movieId, MovieStatus.HIDDEN);
    }

    // JOB-03: phim Sắp chiếu tới ngày khởi chiếu -> Đang chiếu. Trả về tên các phim đã chuyển
    public List<String> promoteReleasedMovies() throws SQLException {
        return movieDAO.promoteReleased(LocalDate.now());
    }

    // JOB-03: phim Đang chiếu đã qua ngày kết thúc chiếu -> Đã kết thúc. Trả về tên các phim đã chuyển
    public List<String> endExpiredMovies() throws SQLException {
        return movieDAO.endExpired(LocalDate.now());
    }

    private Map<String, String> validate(MovieForm form, Movie current) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        int movieId = current == null ? 0 : current.getMovieId();

        // Cần năm khởi chiếu trước để kiểm tra trùng tên
        LocalDate releaseDate = validateReleaseDate(form, errors);
        String title = form.getTitle().trim();
        if (title.isEmpty()) {
            errors.put("title", Messages.format(Messages.MSG02, "Tên phim"));
        } else if (title.length() > MAX_TITLE) {
            errors.put("title", Messages.format(Messages.MSG08, MAX_TITLE));
        } else if (releaseDate != null && movieDAO.existsByTitleAndYear(title, releaseDate.getYear(), movieId)) {
            // BR-MOV-01: không trùng theo cặp tên + năm khởi chiếu, phim làm lại khác năm vẫn thêm được
            errors.put("title", duplicateTitleMessage(releaseDate));
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
        if (form.getCountry().trim().length() > MAX_COUNTRY) {
            errors.put("country", Messages.format(Messages.MSG08, MAX_COUNTRY));
        }
        if (form.getLanguage().trim().length() > MAX_LANGUAGE) {
            errors.put("language", Messages.format(Messages.MSG08, MAX_LANGUAGE));
        }
        if (form.getDistributor().trim().length() > MAX_DISTRIBUTOR) {
            errors.put("distributor", Messages.format(Messages.MSG08, MAX_DISTRIBUTOR));
        }

        validateDuration(form, current, errors);

        // GB-03
        if (form.getAgeRating().isEmpty()) {
            errors.put("ageRating", Messages.format(Messages.MSG02, "Phân loại tuổi"));
        } else if (!AgeRating.isValid(form.getAgeRating())) {
            errors.put("ageRating", Messages.MOVIE_AGE_RATING_INVALID);
        }

        LocalDate endDate = validateEndDate(form, current, releaseDate, errors);
        validateStatus(form, current, releaseDate, endDate, errors);
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

    // Ngày kết thúc chiếu không bắt buộc; trả về ngày hợp lệ, hoặc null nếu để trống/có lỗi
    private LocalDate validateEndDate(MovieForm form, Movie current, LocalDate releaseDate, Map<String, String> errors)
            throws SQLException {
        if (form.getEndDate().trim().isEmpty()) {
            return null;
        }
        LocalDate endDate = parseDate(form.getEndDate());
        if (endDate == null) {
            errors.put("endDate", Messages.MOVIE_END_DATE_INVALID);
            return null;
        }
        if (releaseDate != null && endDate.isBefore(releaseDate)) {
            errors.put("endDate", Messages.MOVIE_END_DATE_BEFORE_RELEASE);
            return null;
        }
        // Rút ngắn ngày kết thúc không được bỏ lại suất chiếu nằm ngoài khoảng chiếu (UC-24 cũng chặn tạo mới)
        if (current != null && movieDAO.hasUnfinishedShowtimesAfter(current.getMovieId(), endDate)) {
            errors.put("endDate", Messages.MOVIE_END_DATE_HAS_SHOWTIMES);
            return null;
        }
        return endDate;
    }

    private void validateStatus(MovieForm form, Movie current, LocalDate releaseDate, LocalDate endDate,
                                Map<String, String> errors) throws SQLException {
        String status = form.getStatus();
        if (status.isEmpty()) {
            errors.put("status", Messages.format(Messages.MSG02, "Trạng thái"));
            return;
        }
        if (!MovieStatus.isValid(status)) {
            errors.put("status", Messages.MOVIE_STATUS_INVALID);
            return;
        }
        // Trạng thái chiếu phải khớp khoảng ngày chiếu, đúng quy tắc JOB-03 tự chuyển; Hidden không phụ thuộc ngày.
        // Ended được chọn tay trước ngày kết thúc (ngừng chiếu sớm).
        LocalDate today = LocalDate.now();
        if (releaseDate != null) {
            if (MovieStatus.COMING_SOON.equals(status) && !releaseDate.isAfter(today)) {
                errors.put("status", Messages.MOVIE_COMING_SOON_DATE_PASSED);
                return;
            }
            boolean released = MovieStatus.NOW_SHOWING.equals(status) || MovieStatus.ENDED.equals(status);
            if (released && releaseDate.isAfter(today)) {
                errors.put("status", Messages.MOVIE_NOT_RELEASED_YET);
                return;
            }
        }
        if (MovieStatus.NOW_SHOWING.equals(status) && endDate != null && endDate.isBefore(today)) {
            errors.put("status", Messages.MOVIE_END_DATE_PASSED);
            return;
        }
        // BR-MOV-04: suất chưa hủy vẫn chiếm lịch phòng và tính vào dashboard, nên phải hủy suất trước khi ẩn/kết thúc
        boolean closing = MovieStatus.HIDDEN.equals(status) || MovieStatus.ENDED.equals(status);
        if (closing && current != null && !status.equals(current.getStatus())
                && movieDAO.hasUnfinishedShowtimes(current.getMovieId())) {
            errors.put("status", Messages.MOVIE_HAS_ACTIVE_SHOWTIMES);
        }
    }

    // Poster chỉ lấy từ file tải lên trong máy (không nhập URL). Không chọn file mới thì posterUrl (ô ẩn) chỉ được là
    // rỗng (bỏ poster) hoặc giữ nguyên poster hiện tại của chính phim đó - tránh sửa form để trỏ sang (rồi xóa nhầm)
    // poster phim khác. Phim cũ đang dùng URL ngoài vẫn giữ được nếu không đổi poster.
    private void validatePoster(MovieForm form, Movie current, Map<String, String> errors) {
        if (!form.getUploadedPoster().isEmpty()) {
            return;
        }
        String poster = form.getPosterUrl().trim();
        if (!poster.isEmpty() && (current == null || !poster.equals(current.getPosterUrl()))) {
            errors.put("poster", Messages.MOVIE_POSTER_INVALID);
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
                // Hai người lưu cùng tên + năm đồng thời: chỉ mục UNIQUE(Title, ReleaseYear) chặn ở DB
                if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                    throw new ValidationException(Map.of("title", duplicateTitleMessage(movie.getReleaseDate())));
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
        movie.setEndDate(parseDate(form.getEndDate()));
        movie.setCountry(blankToNull(form.getCountry()));
        movie.setLanguage(blankToNull(form.getLanguage()));
        movie.setDistributor(blankToNull(form.getDistributor()));
        movie.setPosterUrl(form.getUploadedPoster().isEmpty()
                ? blankToNull(form.getPosterUrl()) : form.getUploadedPoster());
        movie.setTrailerUrl(blankToNull(form.getTrailerUrl()));
        // Lưu theo thứ tự cố định của FORMATS để dữ liệu nhất quán
        String formats = FORMATS.stream().filter(form.getFormats()::contains).collect(Collectors.joining(","));
        movie.setFormats(formats.isEmpty() ? null : formats);
        movie.setStatus(form.getStatus());
        return movie;
    }

    // String.valueOf để MessageFormat không chèn dấu phân cách nghìn ("2,019")
    private String duplicateTitleMessage(LocalDate releaseDate) {
        return Messages.format(Messages.MOVIE_TITLE_DUPLICATE, String.valueOf(releaseDate.getYear()));
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
