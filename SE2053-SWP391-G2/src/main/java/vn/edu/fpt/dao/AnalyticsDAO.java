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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.OccupancyStat;
import vn.edu.fpt.model.SalesSummary;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.util.DBContext;

// UC-29: truy vấn tổng hợp cho dashboard thống kê (chỉ đọc)
public class AnalyticsDAO {

    // BR-M-01: chỉ tính đơn đã thanh toán; vé hủy/đã đổi không tính, tiền hoàn bị trừ khỏi doanh thu.
    // Tham số: trạng thái vé còn hiệu lực (2), trạng thái đơn, từ, đến
    private static final String PAID_BOOKINGS = "FROM Booking b "
            + "OUTER APPLY (SELECT SUM(ISNULL(t.RefundAmount, 0)) AS Refund, "
            + "SUM(CASE WHEN t.Status IN (?, ?) THEN 1 ELSE 0 END) AS Tickets "
            + "FROM Ticket t WHERE t.BookingID = b.BookingID) tk "
            + "OUTER APPLY (SELECT SUM(bc.SubtotalPrice) AS Concession "
            + "FROM Booking_Concession bc WHERE bc.BookingID = b.BookingID) cs "
            + "WHERE b.Status = ? AND b.BookingTime >= ? AND b.BookingTime < ? ";

    // Mỗi suất chiếu trong kỳ kèm sức chứa phòng và số vé còn hiệu lực của đơn đã thanh toán.
    // Phòng chưa khai báo ghế thì lấy Room.TotalSeats. Tham số: trạng thái đơn, trạng thái vé (2), suất hủy, từ, đến
    private static final String SHOWTIME_SALES = "FROM Showtime s "
            + "JOIN Movie m ON m.MovieID = s.MovieID "
            + "JOIN Room r ON r.RoomID = s.RoomID "
            + "OUTER APPLY (SELECT COUNT(*) AS Seats FROM Seat st WHERE st.RoomID = s.RoomID) sc "
            + "OUTER APPLY (SELECT COUNT(*) AS Sold, SUM(t.Price) AS Revenue FROM Ticket t "
            + "JOIN Booking b ON b.BookingID = t.BookingID "
            + "WHERE t.ShowtimeID = s.ShowtimeID AND b.Status = ? AND t.Status IN (?, ?)) tk "
            + "WHERE s.Status <> ? AND s.StartTime >= ? AND s.StartTime < ? ";

    private static final String SHOWTIME_TOTALS = "COUNT(*) AS Showtimes, ISNULL(SUM(tk.Sold), 0) AS Sold, "
            + "SUM(COALESCE(NULLIF(sc.Seats, 0), r.TotalSeats)) AS Capacity, ISNULL(SUM(tk.Revenue), 0) AS Revenue ";

    public SalesSummary summarize(LocalDateTime from, LocalDateTime to) throws SQLException {
        String sql = "SELECT COUNT(*) AS Orders, ISNULL(SUM(tk.Tickets), 0) AS Tickets, "
                + "ISNULL(SUM(b.FinalAmount - ISNULL(tk.Refund, 0)), 0) AS Revenue, "
                + "ISNULL(SUM(cs.Concession), 0) AS Concession " + PAID_BOOKINGS;
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindPaidBookings(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                SalesSummary summary = new SalesSummary();
                if (rs.next()) {
                    summary.setOrderCount(rs.getInt("Orders"));
                    summary.setTicketCount(rs.getInt("Tickets"));
                    summary.setRevenue(rs.getBigDecimal("Revenue"));
                    summary.setConcessionRevenue(rs.getBigDecimal("Concession"));
                }
                return summary;
            }
        }
    }

    // Doanh thu theo ngày; byMonth = true thì gộp theo ngày đầu tháng. Ngày không có đơn sẽ không có trong map
    public Map<LocalDate, BigDecimal> revenueByDate(LocalDateTime from, LocalDateTime to, boolean byMonth)
            throws SQLException {
        String bucket = byMonth
                ? "DATEFROMPARTS(YEAR(b.BookingTime), MONTH(b.BookingTime), 1)"
                : "CAST(b.BookingTime AS DATE)";
        String sql = "SELECT " + bucket + " AS Bucket, SUM(b.FinalAmount - ISNULL(tk.Refund, 0)) AS Revenue "
                + PAID_BOOKINGS + "GROUP BY " + bucket;
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindPaidBookings(ps, from, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getDate("Bucket").toLocalDate(), rs.getBigDecimal("Revenue"));
                }
            }
        }
        return result;
    }

    public List<OccupancyStat> occupancyByRoom(LocalDateTime from, LocalDateTime to) throws SQLException {
        return occupancy("r.RoomName", "r.RoomName", from, to);
    }

    // BR-M-02: phim bán nhiều vé nhất đứng trước
    public List<OccupancyStat> occupancyByMovie(LocalDateTime from, LocalDateTime to) throws SQLException {
        return occupancy("m.Title", "Sold DESC, m.Title", from, to);
    }

    // Giá trị số nguyên trong SystemConfig, trả về defaultValue nếu chưa cấu hình hoặc không phải số
    public int configInt(String key, int defaultValue) throws SQLException {
        String sql = "SELECT ConfigValue FROM SystemConfig WHERE ConfigKey = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Integer.parseInt(rs.getString(1).trim()) : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    // groupColumn và orderBy là chuỗi cố định trong lớp này, không nhận từ người dùng
    private List<OccupancyStat> occupancy(String groupColumn, String orderBy, LocalDateTime from, LocalDateTime to)
            throws SQLException {
        String sql = "SELECT " + groupColumn + " AS Name, " + SHOWTIME_TOTALS + SHOWTIME_SALES
                + "GROUP BY " + groupColumn + " ORDER BY " + orderBy;
        List<OccupancyStat> stats = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BookingStatus.PAID);
            ps.setString(2, TicketStatus.UPCOMING);
            ps.setString(3, TicketStatus.WATCHED);
            ps.setString(4, ShowtimeStatus.CANCELLED);
            ps.setTimestamp(5, Timestamp.valueOf(from));
            ps.setTimestamp(6, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OccupancyStat stat = new OccupancyStat();
                    stat.setName(rs.getString("Name"));
                    stat.setShowtimeCount(rs.getInt("Showtimes"));
                    stat.setTicketCount(rs.getInt("Sold"));
                    stat.setCapacity(rs.getInt("Capacity"));
                    stat.setTicketRevenue(rs.getBigDecimal("Revenue"));
                    stats.add(stat);
                }
            }
        }
        return stats;
    }

    private void bindPaidBookings(PreparedStatement ps, LocalDateTime from, LocalDateTime to) throws SQLException {
        ps.setString(1, TicketStatus.UPCOMING);
        ps.setString(2, TicketStatus.WATCHED);
        ps.setString(3, BookingStatus.PAID);
        ps.setTimestamp(4, Timestamp.valueOf(from));
        ps.setTimestamp(5, Timestamp.valueOf(to));
    }
}
