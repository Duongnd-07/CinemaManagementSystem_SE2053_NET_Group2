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
import vn.edu.fpt.util.DBContext;

public class MovieDAO {

    private static final String SELECT_COLUMNS = "SELECT MovieID, Title, Synopsis, Director, [Cast], DurationMinutes, "
            + "AgeRating, ReleaseDate, PosterUrl, TrailerUrl, Formats, Status FROM Movie ";

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

    public boolean existsByTitle(String title, int excludeMovieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Movie WHERE LOWER(Title) = LOWER(?) AND MovieID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, excludeMovieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // BR-MOV-04: suất chiếu chưa kết thúc đã có vé xác nhận
    public boolean hasActiveBookedShowtimes(int movieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Showtime s "
                + "JOIN Booking b ON b.ShowtimeID = s.ShowtimeID "
                + "JOIN Ticket t ON t.BookingID = b.BookingID "
                + "WHERE s.MovieID = ? AND s.EndTime > GETDATE() AND t.Status = 'Upcoming'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
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
            String pattern = "%" + keyword + "%";
            ps.setString(idx++, pattern);
            ps.setString(idx++, pattern);
            ps.setString(idx++, pattern);
        }
        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }
        return idx;
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
