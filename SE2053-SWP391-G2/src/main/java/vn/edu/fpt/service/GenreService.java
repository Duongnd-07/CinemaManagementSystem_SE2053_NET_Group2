package vn.edu.fpt.service;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.GenreDAO;
import vn.edu.fpt.model.Genre;
import vn.edu.fpt.util.Messages;

public class GenreService {
    public static final int PAGE_SIZE = 10;
    private static final int MAX_NAME = 50;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;

    private final GenreDAO genreDAO = new GenreDAO();

    public GenrePage list(String keyword, int page) throws SQLException {
        String kw = keyword == null ? "" : keyword.trim();
        int total = genreDAO.count(kw);
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        List<Genre> genres = genreDAO.search(kw, (currentPage - 1) * PAGE_SIZE, PAGE_SIZE);
        return new GenrePage(genres, currentPage, PAGE_SIZE, total);
    }

    public Genre findById(int genreId) throws SQLException {
        return genreDAO.findById(genreId);
    }

    // E1: map rỗng nghĩa là hợp lệ
    public Map<String, String> validate(int genreId, String name) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            errors.put("name", Messages.format(Messages.MSG02, "Tên thể loại"));
        } else if (trimmed.length() > MAX_NAME) {
            errors.put("name", Messages.format(Messages.MSG08, MAX_NAME));
        } else if (genreDAO.existsByName(trimmed, genreId)) {
            // BR-GEN-01
            errors.put("name", Messages.GENRE_NAME_DUPLICATE);
        }
        return errors;
    }

    public void save(int genreId, String name) throws SQLException, ValidationException {
        Map<String, String> errors = validate(genreId, name);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String trimmed = name.trim();
        if (genreId > 0 && genreDAO.findById(genreId) == null) {
            throw new ValidationException(Map.of("general", Messages.GENRE_NOT_FOUND));
        }
        try {
            if (genreId <= 0) {
                genreDAO.insert(trimmed);
            } else {
                genreDAO.update(genreId, trimmed);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                throw new ValidationException(Map.of("name", Messages.GENRE_NAME_DUPLICATE));
            }
            throw e;
        }
    }

    // E2 / BR-GEN-03: không xóa thể loại đang gán cho phim
    public void delete(int genreId) throws SQLException, ValidationException {
        Genre genre = genreDAO.findById(genreId);
        if (genre == null) {
            throw new ValidationException(Map.of("general", Messages.GENRE_NOT_FOUND));
        }
        if (!genreDAO.deleteIfUnused(genreId)) {
            Genre current = genreDAO.findById(genreId);
            int used = current == null ? genre.getMovieCount() : current.getMovieCount();
            throw new ValidationException(Map.of("general", Messages.format(Messages.GENRE_IN_USE, used)));
        }
    }
}
