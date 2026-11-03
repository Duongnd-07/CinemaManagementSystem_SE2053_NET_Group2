package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.service.CounterOrder;
import vn.edu.fpt.util.DBContext;

// Lịch sử bán tại quầy (UC-19): đơn Channel = Counter, lọc theo nhân viên / ca / khoảng thời gian / từ khóa.
// Staff chỉ truyền ca của mình (kiểm tra ở Service); Manager lọc theo ngày
public class CounterHistoryDAO {

    private static final String FROM = "FROM Booking b JOIN Showtime s ON s.ShowtimeID = b.ShowtimeID "
            + "JOIN Movie m ON m.MovieID = s.MovieID JOIN Room r ON r.RoomID = s.RoomID "
            + "LEFT JOIN [User] c ON c.UserID = b.CustomerID LEFT JOIN [User] st ON st.UserID = b.StaffID "
            + "LEFT JOIN Voucher v ON v.VoucherID = b.VoucherID LEFT JOIN Shift sh ON sh.ShiftID = b.ShiftID "
            + "OUTER APPLY (SELECT SUM(ISNULL(t.RefundAmount, 0)) AS Refund FROM Ticket t "
            + "WHERE t.BookingID = b.BookingID) rf ";

    // Bộ lọc; trường null / rỗng thì bỏ qua
    public static final class Filter {
        private Integer staffId;
        private Integer shiftId;
        private LocalDateTime from;
        private LocalDateTime to;
        private String keyword = "";

        public Filter staff(Integer staffId) {
            this.staffId = staffId;
            return this;
        }

        public Filter shift(Integer shiftId) {
            this.shiftId = shiftId;
            return this;
        }

        public Filter period(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
            return this;
        }

        public Filter keyword(String keyword) {
            this.keyword = keyword == null ? "" : keyword.trim();
            return this;
        }
    }

    // Tổng của mọi đơn khớp bộ lọc: {số đơn, thực thu, tiền đã hoàn}
    public BigDecimal[] summarize(Filter filter) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*), ISNULL(SUM(b.FinalAmount), 0), ISNULL(SUM(rf.Refund), 0) " + FROM
                + where(filter, params);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new BigDecimal[]{BigDecimal.valueOf(rs.getInt(1)), rs.getBigDecimal(2), rs.getBigDecimal(3)};
            }
        }
    }

    // Đơn mới nhất trước, kèm danh sách vé của từng đơn
    public List<CounterOrder> search(Filter filter, int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT b.BookingID, b.BookingTime, b.Status, b.ShiftID, sh.CounterNo, st.FullName AS StaffName, "
                + "b.CustomerID, c.FullName AS CustomerName, c.Phone, c.Email, m.Title, r.RoomName, s.StartTime, "
                + "b.TotalPrice, b.DiscountAmount, v.VoucherCode, b.PointsRedeemed, b.PointsEarned, b.FinalAmount, "
                + "b.PaymentMethod, b.PaymentReference, b.CashReceived, ISNULL(rf.Refund, 0) AS Refund " + FROM
                + where(filter, params) + "ORDER BY b.BookingTime DESC, b.BookingID DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        params.add(offset);
        params.add(limit);
        Map<Integer, CounterOrder> orders = new LinkedHashMap<>();
        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CounterOrder order = mapOrder(rs);
                        orders.put(order.getBookingId(), order);
                    }
                }
            }
            if (!orders.isEmpty()) {
                loadTickets(conn, orders);
            }
        }
        return new ArrayList<>(orders.values());
    }

    // Vé của các đơn trong trang; ghép bằng Java vì danh sách BookingID đã giới hạn theo trang
    private void loadTickets(Connection conn, Map<Integer, CounterOrder> orders) throws SQLException {
        String placeholders = String.join(", ", Collections.nCopies(orders.size(), "?"));
        String sql = "SELECT t.BookingID, se.RowName, se.ColNumber, t.TicketCode, t.Status, t.RefundAmount "
                + "FROM Ticket t JOIN Seat se ON se.SeatID = t.SeatID WHERE t.BookingID IN (" + placeholders + ") "
                + "ORDER BY t.BookingID, t.TicketID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer bookingId : orders.keySet()) {
                ps.setInt(idx++, bookingId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.get(rs.getInt("BookingID")).getTickets().add(new CounterOrder.Ticket(
                            rs.getString("RowName") + rs.getInt("ColNumber"), rs.getString("TicketCode"),
                            rs.getString("Status"), rs.getBigDecimal("RefundAmount")));
                }
            }
        }
    }

    // Từ khóa khớp mã đơn (#45 hoặc 45), mã vé, SĐT / email / tên khách
    private String where(Filter filter, List<Object> params) {
        StringBuilder sql = new StringBuilder("WHERE b.Channel = ? ");
        params.add(BookingChannel.COUNTER);
        if (filter.staffId != null) {
            sql.append("AND b.StaffID = ? ");
            params.add(filter.staffId);
        }
        if (filter.shiftId != null) {
            sql.append("AND b.ShiftID = ? ");
            params.add(filter.shiftId);
        }
        if (filter.from != null && filter.to != null) {
            sql.append("AND b.BookingTime >= ? AND b.BookingTime < ? ");
            params.add(Timestamp.valueOf(filter.from));
            params.add(Timestamp.valueOf(filter.to));
        }
        if (!filter.keyword.isEmpty()) {
            String keyword = filter.keyword.startsWith("#") ? filter.keyword.substring(1) : filter.keyword;
            sql.append("AND (CAST(b.BookingID AS VARCHAR(12)) = ? OR c.Phone = ? OR c.Email = ? "
                    + "OR c.FullName LIKE ? OR EXISTS (SELECT 1 FROM Ticket tk WHERE tk.BookingID = b.BookingID "
                    + "AND tk.TicketCode = ?)) ");
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add("%" + keyword + "%");
            params.add(keyword.toUpperCase());
        }
        return sql.toString();
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private CounterOrder mapOrder(ResultSet rs) throws SQLException {
        CounterOrder order = new CounterOrder();
        order.setBookingId(rs.getInt("BookingID"));
        order.setBookingTime(rs.getTimestamp("BookingTime").toLocalDateTime());
        order.setStatus(rs.getString("Status"));
        order.setShiftId(rs.getObject("ShiftID", Integer.class));
        order.setCounterNo(rs.getString("CounterNo"));
        order.setStaffName(rs.getString("StaffName"));
        order.setCustomerId(rs.getObject("CustomerID", Integer.class));
        order.setCustomerName(rs.getString("CustomerName"));
        order.setCustomerPhone(rs.getString("Phone"));
        order.setCustomerEmail(rs.getString("Email"));
        order.setMovieTitle(rs.getString("Title"));
        order.setRoomName(rs.getString("RoomName"));
        order.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
        order.setTotalPrice(rs.getBigDecimal("TotalPrice"));
        order.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        order.setVoucherCode(rs.getString("VoucherCode"));
        order.setPointsRedeemed(rs.getInt("PointsRedeemed"));
        order.setPointsEarned(rs.getInt("PointsEarned"));
        order.setFinalAmount(rs.getBigDecimal("FinalAmount"));
        order.setPaymentMethod(rs.getString("PaymentMethod"));
        order.setPaymentReference(rs.getString("PaymentReference"));
        order.setCashReceived(rs.getBigDecimal("CashReceived"));
        order.setRefundTotal(rs.getBigDecimal("Refund"));
        return order;
    }
}
