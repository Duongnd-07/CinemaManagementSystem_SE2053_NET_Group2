package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.OnlineSalesDetail;
import vn.edu.fpt.model.OnlineSalesFilter;
import vn.edu.fpt.model.OnlineSalesRow;
import vn.edu.fpt.model.OnlineSalesSummary;

// UC-37: aggregate at booking level before joining one-to-many records.
public class OnlineSalesReportDAO {
    private static final String CONTEXT = " JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
            + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
            + "LEFT JOIN [User] u ON u.UserID = b.CustomerID ";
    private static final String ORIGINAL_TICKET = "NOT EXISTS (SELECT 1 FROM TicketExchange x "
            + "WHERE t.TicketID = x.NewTicketID OR t.TicketID = x.NewTicketID2)";
    private static final String ORDER_FROM = " FROM Booking b " + CONTEXT
            + "OUTER APPLY (SELECT COUNT(*) AS Tickets, ISNULL(SUM(t.Price), 0) AS TicketValue "
            + "FROM Ticket t WHERE t.BookingID = b.BookingID AND " + ORIGINAL_TICKET + ") tk "
            + "OUTER APPLY (SELECT ISNULL(SUM(c.SubtotalPrice), 0) AS ConcessionValue "
            + "FROM Booking_Concession c WHERE c.BookingID = b.BookingID) cs ";
    private static final String ORDER_SELECT = "SELECT b.BookingID, b.BookingTime, b.PaidAt, b.Status, "
            + "b.PaymentMethod, b.FinalAmount, b.DiscountAmount, m.Title, r.RoomName, s.StartTime, "
            + "u.FullName, tk.Tickets, tk.TicketValue, cs.ConcessionValue ";
    private static final String PAID = " AND b.Status = 'Paid' AND (b.FinalAmount = 0 OR EXISTS "
            + "(SELECT 1 FROM OnlinePayment pay WHERE pay.BookingID = b.BookingID AND pay.Status = 'Paid' "
            + "AND pay.Amount = b.FinalAmount AND pay.ReceivedAmount = b.FinalAmount AND pay.PaidAt IS NOT NULL)) ";
    public OnlineSalesSummary summarize(Connection conn, OnlineSalesFilter filter) throws SQLException {
        OnlineSalesSummary result = new OnlineSalesSummary();
        Query q = where(filter);
        String sql = "SELECT COUNT(*) AS Orders, ISNULL(SUM(tk.Tickets), 0) AS Tickets, "
                + "ISNULL(SUM(tk.TicketValue), 0) AS TicketValue, ISNULL(SUM(cs.ConcessionValue), 0) AS ConcessionValue, "
                + "ISNULL(SUM(b.FinalAmount), 0) AS Collected "
                + ORDER_FROM + q.sql + PAID;
        try (PreparedStatement ps = prepare(conn, sql, q.params); ResultSet rs = ps.executeQuery()) {
            rs.next();
            result.setOrderCount(rs.getInt("Orders"));
            result.setTicketCount(rs.getInt("Tickets"));
            result.setTicketValue(rs.getBigDecimal("TicketValue"));
            result.setConcessionValue(rs.getBigDecimal("ConcessionValue"));
            result.setCollected(rs.getBigDecimal("Collected"));
        }
        return result;
    }

    public Map<LocalDate, BigDecimal> dailyCollections(Connection conn, OnlineSalesFilter f) throws SQLException {
        Query q = where(f);
        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        String sql = "SELECT CAST(b.PaidAt AS DATE) AS Day, SUM(b.FinalAmount) AS Amount FROM Booking b "
                + q.sql + PAID + " GROUP BY CAST(b.PaidAt AS DATE) ORDER BY Day";
        try (PreparedStatement ps = prepare(conn, sql, q.params); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getDate("Day").toLocalDate(), rs.getBigDecimal("Amount"));
            }
        }
        return result;
    }

    public int count(Connection conn, OnlineSalesFilter f) throws SQLException {
        Query q = where(f);
        try (PreparedStatement ps = prepare(conn, "SELECT COUNT(*) " + ORDER_FROM + q.sql + PAID, q.params);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<OnlineSalesRow> rows(Connection conn, OnlineSalesFilter f, int offset, int limit) throws SQLException {
        Query q = where(f);
        String sql = ORDER_SELECT + ORDER_FROM + q.sql + PAID
                + " ORDER BY b.PaidAt DESC, b.BookingID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        q.params.add(offset);
        q.params.add(limit);
        try (PreparedStatement ps = prepare(conn, sql, q.params); ResultSet rs = ps.executeQuery()) {
            List<OnlineSalesRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(order(rs));
            }
            return rows;
        }
    }

    public OnlineSalesDetail detail(Connection conn, int bookingId) throws SQLException {
        OnlineSalesDetail detail = new OnlineSalesDetail();
        try (PreparedStatement ps = prepare(conn, ORDER_SELECT + ORDER_FROM
                + " WHERE b.Channel = 'Online' AND b.PaidAt IS NOT NULL AND b.BookingID = ?" + PAID, List.of(bookingId));
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            detail.setOrder(order(rs));
        }
        List<OnlineSalesRow> tickets = new ArrayList<>();
        String sql = "SELECT t.TicketCode, t.Status, t.Price, CONCAT(se.RowName, se.ColNumber) AS Seat, "
                + "CASE WHEN " + ORIGINAL_TICKET + " THEN 1 ELSE 0 END AS Original "
                + "FROM Ticket t JOIN Seat se ON se.SeatID = t.SeatID WHERE t.BookingID = ? ORDER BY t.TicketID";
        try (PreparedStatement ps = prepare(conn, sql, List.of(bookingId)); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OnlineSalesRow row = new OnlineSalesRow();
                row.setReference(rs.getString("TicketCode"));
                row.setStatus(rs.getString("Status"));
                row.setAmount(money(rs, "Price"));
                row.setDescription(rs.getString("Seat") + (rs.getBoolean("Original") ? " · Vé gốc" : " · Vé đổi"));
                tickets.add(row);
            }
        }
        detail.setTickets(tickets);
        List<OnlineSalesRow> concessions = new ArrayList<>();
        try (PreparedStatement ps = prepare(conn, "SELECT c.Name, bc.Quantity, bc.SubtotalPrice "
                + "FROM Booking_Concession bc JOIN Concession c ON c.ConcessionID = bc.ConcessionID "
                + "WHERE bc.BookingID = ? ORDER BY bc.ConcessionID", List.of(bookingId)); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OnlineSalesRow row = new OnlineSalesRow();
                row.setDescription(rs.getString("Name"));
                row.setTicketCount(rs.getInt("Quantity"));
                row.setAmount(money(rs, "SubtotalPrice"));
                concessions.add(row);
            }
        }
        detail.setConcessions(concessions);
        return detail;
    }

    private Query where(OnlineSalesFilter f) {
        Query q = new Query(" WHERE b.Channel = 'Online' AND b.PaidAt >= ? AND b.PaidAt < ? ");
        q.params.add(f.getFrom().atStartOfDay());
        q.params.add(f.getTo().plusDays(1).atStartOfDay());
        return q;
    }

    private OnlineSalesRow order(ResultSet rs) throws SQLException {
        OnlineSalesRow row = context(rs);
        row.setCreatedAt(time(rs, "BookingTime"));
        row.setPaidAt(time(rs, "PaidAt"));
        row.setOccurredAt(row.getPaidAt());
        row.setStatus(rs.getString("Status"));
        row.setMethod(rs.getString("PaymentMethod"));
        row.setCustomerName(rs.getString("FullName"));
        row.setTicketCount(rs.getInt("Tickets"));
        row.setTicketValue(money(rs, "TicketValue"));
        row.setConcessionValue(money(rs, "ConcessionValue"));
        row.setDiscount(money(rs, "DiscountAmount"));
        row.setAmount(money(rs, "FinalAmount"));
        return row;
    }

    private OnlineSalesRow context(ResultSet rs) throws SQLException {
        OnlineSalesRow row = new OnlineSalesRow();
        row.setBookingId(rs.getInt("BookingID"));
        row.setTitle(rs.getString("Title"));
        row.setRoomName(rs.getString("RoomName"));
        row.setShowtimeAt(time(rs, "StartTime"));
        return row;
    }

    private BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    private PreparedStatement prepare(Connection conn, String sql, List<Object> params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof LocalDateTime time) {
                    ps.setTimestamp(i + 1, Timestamp.valueOf(time));
                } else {
                    ps.setObject(i + 1, value);
                }
            }
            return ps;
        } catch (SQLException e) {
            ps.close();
            throw e;
        }
    }

    private static final class Query {
        private String sql;
        private final List<Object> params = new ArrayList<>();
        private Query(String sql) { this.sql = sql; }
    }
}
