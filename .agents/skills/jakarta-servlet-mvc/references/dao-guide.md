# DAO & Database Access Reference Guide

This guide defines the standards for Data Access Objects (DAOs) using HikariCP and SQL Server in `SE2053-SWP391-G2`.

---

## 1. Standard DAO Template (Read & Pagination)

```java
package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.util.DBContext;

public class MovieDAO {
    private static final Logger LOGGER = Logger.getLogger(MovieDAO.class.getName());

    public Optional<Movie> findById(int id) {
        String sql = "SELECT movie_id, title, duration, release_date, poster_url, status " +
                     "FROM Movies WHERE movie_id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding movie by id=" + id, e);
        }
        return Optional.empty();
    }

    public List<Movie> findWithPagination(String keyword, int offset, int limit) {
        List<Movie> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT movie_id, title, duration, release_date, poster_url, status " +
            "FROM Movies WHERE 1=1 "
        );

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND title LIKE ? ");
        }
        sql.append("ORDER BY movie_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + keyword.trim() + "%");
            }
            ps.setInt(paramIndex++, offset);
            ps.setInt(paramIndex, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMovie(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching paginated movies", e);
        }
        return list;
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setMovieId(rs.getInt("movie_id"));
        m.setTitle(rs.getString("title"));
        m.setDuration(rs.getInt("duration"));
        m.setReleaseDate(rs.getDate("release_date"));
        m.setPosterUrl(rs.getString("poster_url"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
```

---

## 2. Multi-Step Transaction Pattern

When executing multiple operations that must succeed or fail together (e.g. creating a booking and booking details):

```java
public boolean processBooking(Booking booking, List<BookingDetail> details) {
    String insertBookingSql = "INSERT INTO Bookings (user_id, showtime_id, total_price, status) VALUES (?, ?, ?, ?)";
    String insertDetailSql = "INSERT INTO BookingDetails (booking_id, seat_id, price) VALUES (?, ?, ?)";

    Connection conn = null;
    try {
        conn = DBContext.getConnection();
        conn.setAutoCommit(false); // Start transaction

        int bookingId;
        try (PreparedStatement ps = conn.prepareStatement(insertBookingSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getUserId());
            ps.setInt(2, booking.getShowtimeId());
            ps.setBigDecimal(3, booking.getTotalPrice());
            ps.setString(4, booking.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    bookingId = rs.getInt(1);
                } else {
                    throw new SQLException("Creating booking failed, no ID obtained.");
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(insertDetailSql)) {
            for (BookingDetail detail : details) {
                ps.setInt(1, bookingId);
                ps.setInt(2, detail.getSeatId());
                ps.setBigDecimal(3, detail.getPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        conn.commit(); // Commit transaction
        return true;
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Transaction failed for processBooking. Rolling back.", e);
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed", ex);
            }
        }
        return false;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close(); // Return to pool
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }
}
```

---

## 3. Critical Rules

1. **Always use PreparedStatement parameters:** Never construct queries using string concatenation (e.g., `"... WHERE id = " + id`).
2. **Never leave Connections unclosed:** If an exception is thrown, an unclosed connection will hang in the pool. Always use `try-with-resources` or a `finally` block with `conn.close()`.
3. **Handle SQL Server types properly:**
   - Date / Datetime: `rs.getTimestamp(...)` or `rs.getDate(...)`
   - BigDecimal for money: `rs.getBigDecimal(...)`
   - NCHAR / NVARCHAR: `rs.getNString(...)` or `rs.getString(...)`
