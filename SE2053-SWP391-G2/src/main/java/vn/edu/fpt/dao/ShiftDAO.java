package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.PaymentMethod;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.service.ShiftReport;
import vn.edu.fpt.util.DBContext;

// UC-19/UC-22: ca làm việc tại quầy. Mỗi nhân viên chỉ một ca đang mở (UX_Shift_OpenPerStaff)
public class ShiftDAO {

    private static final String SELECT_FROM = "SELECT sh.ShiftID, sh.StaffID, sh.CounterNo, sh.StartedAt, sh.EndedAt, "
            + "sh.TicketCount, sh.TicketRevenue, sh.ConcessionRevenue, sh.CashTotal, sh.CardTotal, sh.TransferTotal, "
            + "sh.RefundTotal, sh.ClosedAt, sh.OpeningCash, sh.CountedCash, sh.CashNote, sh.ClosedBy, "
            + "st.FullName AS StaffName, cb.FullName AS ClosedByName FROM Shift sh ";
    private static final String JOIN_NAMES = "JOIN [User] st ON st.UserID = sh.StaffID "
            + "LEFT JOIN [User] cb ON cb.UserID = sh.ClosedBy ";
    private static final String LOCK = "WITH (UPDLOCK, ROWLOCK) ";

    public Shift findOpenByStaff(int staffId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findOpenByStaff(conn, staffId, false);
        }
    }

    // lock = true khi gọi trong transaction (bán vé, kết ca) để hai thao tác không chen nhau
    public Shift findOpenByStaff(Connection conn, int staffId, boolean lock) throws SQLException {
        return findOne(conn, SELECT_FROM + (lock ? LOCK : "") + JOIN_NAMES
                + "WHERE sh.StaffID = ? AND sh.EndedAt IS NULL", staffId);
    }

    public Shift findById(int shiftId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return findById(conn, shiftId, false);
        }
    }

    public Shift findById(Connection conn, int shiftId, boolean lock) throws SQLException {
        return findOne(conn, SELECT_FROM + (lock ? LOCK : "") + JOIN_NAMES + "WHERE sh.ShiftID = ?", shiftId);
    }

    public void open(int staffId, String counterNo, BigDecimal openingCash) throws SQLException {
        String sql = "INSERT INTO Shift (StaffID, CounterNo, StartedAt, OpeningCash) VALUES (?, ?, GETDATE(), ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, counterNo);
            ps.setBigDecimal(3, openingCash);
            ps.executeUpdate();
        }
    }

    // Ca đang mở đã quá thời lượng tối đa (tính theo đồng hồ DB như mọi mốc thời gian khác)
    public List<Integer> findExpiredOpenIds(Connection conn, int shiftHours) throws SQLException {
        String sql = "SELECT ShiftID FROM Shift WHERE EndedAt IS NULL AND DATEADD(HOUR, ?, StartedAt) <= GETDATE()";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shiftHours);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    // Tổng các đơn Paid bán tại quầy trong ca (đơn tại quầy luôn thu đủ tiền ngay nên không có đơn Pending)
    public void summarize(Connection conn, int shiftId, ShiftReport report) throws SQLException {
        String sql = "SELECT COUNT(*) AS Orders, ISNULL(SUM(tk.Tickets), 0) AS Tickets, "
                + "ISNULL(SUM(tk.TicketRevenue), 0) AS TicketRevenue, ISNULL(SUM(cs.Revenue), 0) AS ConcessionRevenue, "
                + "ISNULL(SUM(cs.Items), 0) AS Items, ISNULL(SUM(b.DiscountAmount), 0) AS Discount, "
                + "ISNULL(SUM(CASE WHEN b.PaymentMethod = ? THEN b.FinalAmount END), 0) AS Cash, "
                + "ISNULL(SUM(CASE WHEN b.PaymentMethod = ? THEN b.FinalAmount END), 0) AS Card, "
                + "ISNULL(SUM(CASE WHEN b.PaymentMethod = ? THEN b.FinalAmount END), 0) AS Transfer "
                + "FROM Booking b "
                // Chỉ vé bán ra lúc đầu: vé cấp mới khi đổi ghế (UC-20) không phải doanh thu mới
                + "OUTER APPLY (SELECT COUNT(*) AS Tickets, SUM(t.Price) AS TicketRevenue "
                + "FROM Ticket t WHERE t.BookingID = b.BookingID AND NOT EXISTS (SELECT 1 FROM TicketExchange x "
                + "WHERE t.TicketID IN (x.NewTicketID, x.NewTicketID2))) tk "
                + "OUTER APPLY (SELECT SUM(bc.SubtotalPrice) AS Revenue, SUM(bc.Quantity) AS Items "
                + "FROM Booking_Concession bc WHERE bc.BookingID = b.BookingID) cs "
                + "WHERE b.ShiftID = ? AND b.Status = ? AND b.Channel = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentMethod.CASH);
            ps.setString(2, PaymentMethod.CARD);
            ps.setString(3, PaymentMethod.TRANSFER);
            ps.setInt(4, shiftId);
            ps.setString(5, BookingStatus.PAID);
            ps.setString(6, BookingChannel.COUNTER);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                report.setOrderCount(rs.getInt("Orders"));
                report.setTicketCount(rs.getInt("Tickets"));
                report.setTicketRevenue(rs.getBigDecimal("TicketRevenue"));
                report.setConcessionRevenue(rs.getBigDecimal("ConcessionRevenue"));
                report.setConcessionItems(rs.getInt("Items"));
                report.setDiscountTotal(rs.getBigDecimal("Discount"));
                report.setCashTotal(rs.getBigDecimal("Cash"));
                report.setCardTotal(rs.getBigDecimal("Card"));
                report.setTransferTotal(rs.getBigDecimal("Transfer"));
            }
        }
        // Điều chỉnh sau bán thuộc ca đã trả tiền / xử lý, kể cả đơn bán ở ca khác:
        // tiền hoàn tại quầy (Ticket.RefundShiftID) và số lần đổi ghế nhân viên làm trong khung giờ ca
        String adjustSql = "SELECT (SELECT ISNULL(SUM(RefundAmount), 0) FROM Ticket WHERE RefundShiftID = sh.ShiftID) "
                + "AS Refund, (SELECT COUNT(*) FROM Ticket WHERE RefundShiftID = sh.ShiftID) AS Refunded, "
                + "(SELECT COUNT(*) FROM TicketExchange x WHERE x.StaffID = sh.StaffID AND x.ExchangeType <> 'Refund' "
                + "AND x.CreatedAt >= sh.StartedAt AND (sh.EndedAt IS NULL OR x.CreatedAt < sh.EndedAt)) AS Exchanged "
                + "FROM Shift sh WHERE sh.ShiftID = ?";
        try (PreparedStatement ps = conn.prepareStatement(adjustSql)) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                report.setRefundTotal(rs.getBigDecimal("Refund"));
                report.setRefundedTickets(rs.getInt("Refunded"));
                report.setExchangedTickets(rs.getInt("Exchanged"));
            }
        }
    }

    // Đơn của ca kèm 3 cột ghế: ghế lúc bán, đổi ghế (cũ -> mới), vé hoàn tiền mặt (ghế · số tiền).
    // Gộp bằng Java để không phụ thuộc STRING_AGG (SQL Server 2017+)
    public void findOrders(Connection conn, int shiftId, ShiftReport report) throws SQLException {
        Map<Integer, StringBuilder> seats = new LinkedHashMap<>();
        Map<Integer, StringBuilder> exchanges = new LinkedHashMap<>();
        Map<Integer, StringBuilder> refunds = new LinkedHashMap<>();
        String seatSql = "SELECT t.BookingID, CONCAT(se.RowName, se.ColNumber) AS Seat, t.RefundAmount, "
                + "CASE WHEN EXISTS (SELECT 1 FROM TicketExchange n WHERE t.TicketID IN (n.NewTicketID, n.NewTicketID2)) "
                + "THEN 1 ELSE 0 END AS Issued, x.ExchangeID, CONCAT(s1.RowName, s1.ColNumber) AS NewSeat, "
                + "CONCAT(s2.RowName, s2.ColNumber) AS NewSeat2 "
                + "FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID JOIN Seat se ON se.SeatID = t.SeatID "
                + "LEFT JOIN TicketExchange x ON x.OldTicketID = t.TicketID AND x.ExchangeType <> 'Refund' "
                + "LEFT JOIN Ticket n1 ON n1.TicketID = x.NewTicketID LEFT JOIN Seat s1 ON s1.SeatID = n1.SeatID "
                + "LEFT JOIN Ticket n2 ON n2.TicketID = x.NewTicketID2 LEFT JOIN Seat s2 ON s2.SeatID = n2.SeatID "
                + "WHERE b.ShiftID = ? ORDER BY t.BookingID, se.RowName, se.ColNumber";
        try (PreparedStatement ps = conn.prepareStatement(seatSql)) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bookingId = rs.getInt("BookingID");
                    String seat = rs.getString("Seat");
                    // Vé cấp khi đổi ghế không phải ghế bán ra; nó chỉ xuất hiện ở cột đổi ghế
                    if (rs.getInt("Issued") == 0) {
                        append(seats, bookingId, seat);
                    }
                    if (rs.getObject("ExchangeID") != null) {
                        String newSeat2 = rs.getString("NewSeat2");
                        append(exchanges, bookingId, seat + " → " + rs.getString("NewSeat")
                                + (newSeat2.isEmpty() ? "" : " + " + newSeat2));
                    }
                    BigDecimal refund = rs.getBigDecimal("RefundAmount");
                    if (refund != null && refund.signum() > 0) {
                        append(refunds, bookingId, seat + " · "
                                + String.format(Locale.forLanguageTag("vi-VN"), "%,.0fđ", refund));
                    }
                }
            }
        }
        Map<Integer, StringBuilder> concessions = new LinkedHashMap<>();
        String itemSql = "SELECT bc.BookingID, c.Name, bc.Quantity FROM Booking_Concession bc "
                + "JOIN Concession c ON c.ConcessionID = bc.ConcessionID JOIN Booking b ON b.BookingID = bc.BookingID "
                + "WHERE b.ShiftID = ? ORDER BY bc.BookingID, c.Name";
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    append(concessions, rs.getInt("BookingID"), rs.getString("Name") + " × " + rs.getInt("Quantity"));
                }
            }
        }
        String sql = "SELECT b.BookingID, b.BookingTime, b.PaymentMethod, b.PaymentReference, b.FinalAmount, "
                + "c.FullName AS CustomerName "
                + "FROM Booking b LEFT JOIN [User] c ON c.UserID = b.CustomerID "
                + "WHERE b.ShiftID = ? AND b.Status = ? AND b.Channel = ? ORDER BY b.BookingTime DESC, b.BookingID DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shiftId);
            ps.setString(2, BookingStatus.PAID);
            ps.setString(3, BookingChannel.COUNTER);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bookingId = rs.getInt("BookingID");
                    report.getOrders().add(new ShiftReport.Order(bookingId,
                            rs.getTimestamp("BookingTime").toLocalDateTime(), text(seats, bookingId),
                            text(exchanges, bookingId), text(refunds, bookingId), text(concessions, bookingId),
                            rs.getString("CustomerName"), rs.getString("PaymentMethod"), rs.getString("PaymentReference"),
                            rs.getBigDecimal("FinalAmount")));
                }
            }
        }
    }

    private static void append(Map<Integer, StringBuilder> labels, int bookingId, String value) {
        StringBuilder label = labels.computeIfAbsent(bookingId, id -> new StringBuilder());
        label.append(label.length() == 0 ? "" : ", ").append(value);
    }

    private static String text(Map<Integer, StringBuilder> labels, int bookingId) {
        StringBuilder label = labels.get(bookingId);
        return label == null ? "" : label.toString();
    }

    // Kết ca và chốt số liệu (BR-03). autoHours != null: hệ thống tự kết, giờ kết = giờ mở + autoHours;
    // ngược lại giờ kết là hiện tại. countedCash null = chưa đếm két. Trả về false nếu ca đã kết trước đó
    public boolean close(Connection conn, int shiftId, Integer autoHours, Integer closedBy, ShiftReport totals,
                         BigDecimal countedCash, String cashNote) throws SQLException {
        String sql = "UPDATE Shift SET EndedAt = CASE WHEN ? IS NULL THEN GETDATE() ELSE DATEADD(HOUR, ?, StartedAt) END, "
                + "ClosedAt = GETDATE(), ClosedBy = ?, TicketCount = ?, TicketRevenue = ?, ConcessionRevenue = ?, "
                + "CashTotal = ?, CardTotal = ?, TransferTotal = ?, RefundTotal = ?, CountedCash = ?, CashNote = ? "
                + "WHERE ShiftID = ? AND EndedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, autoHours);
            setNullableInt(ps, 2, autoHours);
            setNullableInt(ps, 3, closedBy);
            ps.setInt(4, totals.getTicketCount());
            ps.setBigDecimal(5, totals.getTicketRevenue());
            ps.setBigDecimal(6, totals.getConcessionRevenue());
            ps.setBigDecimal(7, totals.getCashTotal());
            ps.setBigDecimal(8, totals.getCardTotal());
            ps.setBigDecimal(9, totals.getTransferTotal());
            ps.setBigDecimal(10, totals.getRefundTotal());
            ps.setBigDecimal(11, countedCash);
            ps.setString(12, cashNote);
            ps.setInt(13, shiftId);
            return ps.executeUpdate() > 0;
        }
    }

    // Nhập số đếm két cho ca đã kết nhưng chưa đếm (ca tự kết hoặc Manager kết hộ); chỉ được nhập một lần
    public boolean recordCount(int shiftId, BigDecimal countedCash, String cashNote) throws SQLException {
        String sql = "UPDATE Shift SET CountedCash = ?, CashNote = ? "
                + "WHERE ShiftID = ? AND EndedAt IS NOT NULL AND CountedCash IS NULL";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, countedCash);
            ps.setString(2, cashNote);
            ps.setInt(3, shiftId);
            return ps.executeUpdate() > 0;
        }
    }

    // Manager đổi thời lượng ca (dòng SHIFT_HOURS do sql/UC22_shift_cash_count.sql tạo); thêm dòng nếu DB chưa có
    public void saveShiftHours(String configKey, int hours) throws SQLException {
        String sql = "UPDATE SystemConfig SET ConfigValue = ?, UpdatedAt = GETDATE() WHERE ConfigKey = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(hours));
            ps.setString(2, configKey);
            if (ps.executeUpdate() > 0) {
                return;
            }
        }
        String insert = "INSERT INTO SystemConfig (ConfigKey, ConfigValue, Description) VALUES (?, ?, ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, configKey);
            ps.setString(2, String.valueOf(hours));
            ps.setString(3, "UC-22: số giờ tối đa của một ca bán tại quầy, hết giờ hệ thống tự kết ca");
            ps.executeUpdate();
        }
    }

    // shortcut: chỉ lấy 30 ca gần nhất, chưa phân trang; thêm Page khi nhân viên cần xem lịch sử dài hơn
    public List<Shift> findRecentByStaff(int staffId) throws SQLException {
        return findMany(SELECT_FROM + JOIN_NAMES + "WHERE sh.StaffID = ? ORDER BY sh.StartedAt DESC "
                + "OFFSET 0 ROWS FETCH NEXT 30 ROWS ONLY", ps -> ps.setInt(1, staffId));
    }

    // Ca bắt đầu trong khoảng [from, to) của mọi nhân viên, cùng các ca vẫn đang mở từ trước đó
    public List<Shift> findByPeriod(LocalDateTime from, LocalDateTime to) throws SQLException {
        return findMany(SELECT_FROM + JOIN_NAMES + "WHERE (sh.StartedAt >= ? AND sh.StartedAt < ?) "
                + "OR (sh.EndedAt IS NULL AND sh.StartedAt < ?) ORDER BY sh.StartedAt DESC", ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(from));
                    ps.setTimestamp(2, Timestamp.valueOf(to));
                    ps.setTimestamp(3, Timestamp.valueOf(to));
                });
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Shift> findMany(String sql, Binder binder) throws SQLException {
        List<Shift> shifts = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRow(rs));
                }
            }
        }
        return shifts;
    }

    private Shift findOne(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private Shift mapRow(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.setShiftId(rs.getInt("ShiftID"));
        shift.setStaffId(rs.getInt("StaffID"));
        shift.setCounterNo(rs.getString("CounterNo"));
        shift.setStartedAt(toDateTime(rs.getTimestamp("StartedAt")));
        shift.setEndedAt(toDateTime(rs.getTimestamp("EndedAt")));
        shift.setTicketCount(rs.getObject("TicketCount", Integer.class));
        shift.setTicketRevenue(rs.getBigDecimal("TicketRevenue"));
        shift.setConcessionRevenue(rs.getBigDecimal("ConcessionRevenue"));
        shift.setCashTotal(rs.getBigDecimal("CashTotal"));
        shift.setCardTotal(rs.getBigDecimal("CardTotal"));
        shift.setTransferTotal(rs.getBigDecimal("TransferTotal"));
        shift.setRefundTotal(rs.getBigDecimal("RefundTotal"));
        shift.setClosedAt(toDateTime(rs.getTimestamp("ClosedAt")));
        shift.setOpeningCash(rs.getBigDecimal("OpeningCash"));
        shift.setCountedCash(rs.getBigDecimal("CountedCash"));
        shift.setCashNote(rs.getString("CashNote"));
        shift.setClosedBy(rs.getObject("ClosedBy", Integer.class));
        shift.setStaffName(rs.getString("StaffName"));
        shift.setClosedByName(rs.getString("ClosedByName"));
        return shift;
    }

    private LocalDateTime toDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
