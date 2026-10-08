package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.util.DBContext;

public class GenreDAO {

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
}
