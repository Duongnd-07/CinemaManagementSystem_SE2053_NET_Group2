package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.DBContext;

public class MovieDAO {

    private static final String SELECT_COLUMNS = "SELECT MovieID, Title, Synopsis, Director, [Cast], DurationMinutes, "
            + "AgeRating, ReleaseDate, PosterUrl, TrailerUrl, Formats, Status FROM Movie ";
    // CI: không phân biệt hoa thường, AI: không phân biệt dấu
    private static final String SEARCH_COLLATION = "Latin1_General_CI_AI";

    public List<Movie> search(String keyword, String status, String orderBy, int offset, int limit)
            throws SQLException {
        // orderBy là chuỗi cố định do Service chọn từ whitelist, không nhận trực tiếp từ người dùng
        String sql = SELECT_COLUMNS + buildWhere(keyword, status) + " ORDER BY " + orderBy
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Movie> movies = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilter(ps, keyword, status);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapRow(rs));
                }
            }
            loadGenres(conn, movies);
        }
        return movies;
    }

    public int count(String keyword, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Movie " + buildWhere(keyword, status);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, keyword, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> countByStatus(String keyword) throws SQLException {
        String sql = "SELECT Status, COUNT(*) AS Total FROM Movie " + buildWhere(keyword, null) + " GROUP BY Status";
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, keyword, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return result;
    }

    // UC-04/UC-05: phim theo các trạng thái cho trước, kèm bộ lọc (null/rỗng = bỏ qua, các điều kiện kết hợp AND).
    // Chữ được so khớp một phần, không phân biệt hoa thường và dấu ("lat mat" khớp "Lật Mặt"). Mới khởi chiếu xếp trước.
    public List<Movie> findByStatuses(List<String> statuses, String title, Integer genreId, String director,
            String cast, List<String> ageRatings) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        if (statuses.isEmpty()) {
            return movies;
        }
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>(statuses);
        conditions.add("Status IN (" + String.join(",", Collections.nCopies(statuses.size(), "?")) + ")");
        addContains(conditions, params, "Title", title);
        addContains(conditions, params, "Director", director);
        addContains(conditions, params, "[Cast]", cast);
        if (ageRatings != null && !ageRatings.isEmpty()) {
            conditions.add("AgeRating IN (" + String.join(",", Collections.nCopies(ageRatings.size(), "?")) + ")");
            params.addAll(ageRatings);
        }
        if (genreId != null) {
            conditions.add("EXISTS (SELECT 1 FROM Movie_Genre mg WHERE mg.MovieID = Movie.MovieID AND mg.GenreID = ?)");
            params.add(genreId);
        }
        String sql = SELECT_COLUMNS + "WHERE " + String.join(" AND ", conditions) + " ORDER BY ReleaseDate DESC, Title";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapRow(rs));
                }
            }
            loadGenres(conn, movies);
        }
        return movies;
    }

    // UC-05 gợi ý khi gõ: tối đa limit phim có tên chứa từ khóa (không phân biệt hoa thường/dấu),
    // tên bắt đầu bằng từ khóa xếp trước; không nạp thể loại cho nhẹ
    public List<Movie> suggestByTitle(List<String> statuses, String keyword, int limit) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        if (statuses.isEmpty() || keyword == null || keyword.isEmpty()) {
            return movies;
        }
        String escaped = escapeLike(keyword);
        String titleMatch = "Title COLLATE " + SEARCH_COLLATION + " LIKE ?";
        String sql = "SELECT TOP (?) " + SELECT_COLUMNS.substring("SELECT ".length())
                + "WHERE Status IN (" + String.join(",", Collections.nCopies(statuses.size(), "?")) + ") "
                + "AND " + titleMatch + " "
                + "ORDER BY CASE WHEN " + titleMatch + " THEN 0 ELSE 1 END, ReleaseDate DESC, Title";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, limit);
            for (String status : statuses) {
                ps.setString(idx++, status);
            }
            ps.setString(idx++, "%" + escaped + "%");
            ps.setString(idx, escaped + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapRow(rs));
                }
            }
        }
        return movies;
    }

    public Movie findById(int movieId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE MovieID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Movie movie = mapRow(rs);
                loadGenres(conn, Collections.singletonList(movie));
                return movie;
            }
        }
    }

    // So sánh theo collation của cột (CI) để khớp đúng ràng buộc UNIQUE(Title)
    public boolean existsByTitle(String title, int excludeMovieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Movie WHERE Title = ? AND MovieID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, excludeMovieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Suất chưa hủy và chưa kết thúc. Dùng cho GB-08 (khóa thời lượng) và BR-MOV-04 (chặn ẩn/kết thúc phim)
    public boolean hasUnfinishedShowtimes(int movieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Showtime "
                + "WHERE MovieID = ? AND EndTime > GETDATE() AND (Status IS NULL OR Status <> ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setString(2, ShowtimeStatus.CANCELLED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int insert(Connection conn, Movie movie) throws SQLException {
        String sql = "INSERT INTO Movie (Title, Synopsis, Director, [Cast], DurationMinutes, AgeRating, ReleaseDate, "
                + "PosterUrl, TrailerUrl, Formats, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMovie(ps, movie);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được MovieID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    public void update(Connection conn, Movie movie) throws SQLException {
        String sql = "UPDATE Movie SET Title = ?, Synopsis = ?, Director = ?, [Cast] = ?, DurationMinutes = ?, "
                + "AgeRating = ?, ReleaseDate = ?, PosterUrl = ?, TrailerUrl = ?, Formats = ?, Status = ? "
                + "WHERE MovieID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindMovie(ps, movie);
            ps.setInt(idx, movie.getMovieId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int movieId, String status) throws SQLException {
        String sql = "UPDATE Movie SET Status = ? WHERE MovieID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, movieId);
            ps.executeUpdate();
        }
    }

    public void replaceGenres(Connection conn, int movieId, List<Integer> genreIds) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movie_Genre WHERE MovieID = ?")) {
            ps.setInt(1, movieId);
            ps.executeUpdate();
        }
        if (genreIds.isEmpty()) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Movie_Genre (MovieID, GenreID) VALUES (?, ?)")) {
            for (int genreId : genreIds) {
                ps.setInt(1, movieId);
                ps.setInt(2, genreId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private String buildWhere(String keyword, String status) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            conditions.add("(Title LIKE ? OR Director LIKE ? OR [Cast] LIKE ?)");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("Status = ?");
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private int bindFilter(PreparedStatement ps, String keyword, String status) throws SQLException {
        int idx = 1;
        if (keyword != null && !keyword.isEmpty()) {
            String pattern = "%" + escapeLike(keyword) + "%";
            ps.setString(idx++, pattern);
            ps.setString(idx++, pattern);
            ps.setString(idx++, pattern);
        }
        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }
        return idx;
    }

    private void addContains(List<String> conditions, List<Object> params, String column, String value) {
        if (value != null && !value.isEmpty()) {
            conditions.add(column + " COLLATE " + SEARCH_COLLATION + " LIKE ?");
            params.add("%" + escapeLike(value) + "%");
        }
    }

    // Ký tự đại diện của LIKE trong SQL Server được bọc [] để tìm đúng chuỗi người dùng nhập
    private String escapeLike(String value) {
        return value.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }

    private int bindMovie(PreparedStatement ps, Movie movie) throws SQLException {
        int idx = 1;
        ps.setString(idx++, movie.getTitle());
        ps.setString(idx++, movie.getSynopsis());
        ps.setString(idx++, movie.getDirector());
        ps.setString(idx++, movie.getCast());
        ps.setInt(idx++, movie.getDurationMinutes());
        ps.setString(idx++, movie.getAgeRating());
        ps.setDate(idx++, movie.getReleaseDate() == null ? null : Date.valueOf(movie.getReleaseDate()));
        ps.setString(idx++, movie.getPosterUrl());
        ps.setString(idx++, movie.getTrailerUrl());
        ps.setString(idx++, movie.getFormats());
        ps.setString(idx++, movie.getStatus());
        return idx;
    }

    private Movie mapRow(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));
        movie.setSynopsis(rs.getString("Synopsis"));
        movie.setDirector(rs.getString("Director"));
        movie.setCast(rs.getString("Cast"));
        movie.setDurationMinutes(rs.getInt("DurationMinutes"));
        movie.setAgeRating(rs.getString("AgeRating"));
        Date releaseDate = rs.getDate("ReleaseDate");
        movie.setReleaseDate(releaseDate == null ? null : releaseDate.toLocalDate());
        movie.setPosterUrl(rs.getString("PosterUrl"));
        movie.setTrailerUrl(rs.getString("TrailerUrl"));
        movie.setFormats(rs.getString("Formats"));
        movie.setStatus(rs.getString("Status"));
        return movie;
    }

    private void loadGenres(Connection conn, List<Movie> movies) throws SQLException {
        if (movies.isEmpty()) {
            return;
        }
        Map<Integer, Movie> byId = new LinkedHashMap<>();
        for (Movie movie : movies) {
            byId.put(movie.getMovieId(), movie);
        }
        String sql = "SELECT mg.MovieID, g.GenreID, g.GenreName FROM Movie_Genre mg "
                + "JOIN Genre g ON g.GenreID = mg.GenreID WHERE mg.MovieID IN ("
                + String.join(",", Collections.nCopies(byId.size(), "?")) + ") ORDER BY g.GenreName";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (int movieId : byId.keySet()) {
                ps.setInt(idx++, movieId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Genre genre = new Genre();
                    genre.setGenreId(rs.getInt("GenreID"));
                    genre.setGenreName(rs.getString("GenreName"));
                    byId.get(rs.getInt("MovieID")).getGenres().add(genre);
                }
            }
        }
    }
}
