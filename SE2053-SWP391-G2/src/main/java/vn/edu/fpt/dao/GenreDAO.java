package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.util.DBContext;

public class GenreDAO {

    private static final String SELECT_WITH_COUNT = "SELECT g.GenreID, g.GenreName, "
            + "(SELECT COUNT(*) FROM Movie_Genre mg WHERE mg.GenreID = g.GenreID) AS MovieCount FROM Genre g ";

    public List<Genre> findAll() throws SQLException {
        String sql = "SELECT GenreID, GenreName FROM Genre ORDER BY GenreName";
        List<Genre> genres = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Genre genre = new Genre();
                genre.setGenreId(rs.getInt("GenreID"));
                genre.setGenreName(rs.getString("GenreName"));
                genres.add(genre);
            }
        }
        return genres;
    }

    public List<Genre> search(String keyword, int offset, int limit) throws SQLException {
        String sql = SELECT_WITH_COUNT + buildWhere(keyword)
                + "ORDER BY g.GenreName OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Genre> genres = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilter(ps, keyword);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    genres.add(mapRow(rs));
                }
            }
        }
        return genres;
    }

    public int count(String keyword) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Genre g " + buildWhere(keyword);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Genre findById(int genreId) throws SQLException {
        String sql = SELECT_WITH_COUNT + "WHERE g.GenreID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, genreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean existsByName(String name, int excludeGenreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Genre WHERE LOWER(GenreName) = LOWER(?) AND GenreID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeGenreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int insert(String name) throws SQLException {
        String sql = "INSERT INTO Genre (GenreName) VALUES (?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(int genreId, String name) throws SQLException {
        String sql = "UPDATE Genre SET GenreName = ? WHERE GenreID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, genreId);
            ps.executeUpdate();
        }
    }

    // BR-GEN-03: điều kiện NOT EXISTS nằm trong cùng câu lệnh để tránh race với việc gán thể loại cho phim
    public boolean deleteIfUnused(int genreId) throws SQLException {
        String sql = "DELETE FROM Genre WHERE GenreID = ? "
                + "AND NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE GenreID = ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, genreId);
            ps.setInt(2, genreId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countExisting(List<Integer> genreIds) throws SQLException {
        if (genreIds.isEmpty()) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM Genre WHERE GenreID IN ("
                + String.join(",", Collections.nCopies(genreIds.size(), "?")) + ")";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < genreIds.size(); i++) {
                ps.setInt(i + 1, genreIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String buildWhere(String keyword) {
        return keyword == null || keyword.isEmpty() ? "" : "WHERE g.GenreName LIKE ? ";
    }

    private int bindFilter(PreparedStatement ps, String keyword) throws SQLException {
        int idx = 1;
        if (keyword != null && !keyword.isEmpty()) {
            ps.setString(idx++, "%" + keyword + "%");
        }
        return idx;
    }

    private Genre mapRow(ResultSet rs) throws SQLException {
        Genre genre = new Genre();
        genre.setGenreId(rs.getInt("GenreID"));
        genre.setGenreName(rs.getString("GenreName"));
        genre.setMovieCount(rs.getInt("MovieCount"));
        return genre;
    }
}
