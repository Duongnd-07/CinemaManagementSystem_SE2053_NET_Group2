package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.model.BookingConcession;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.ConcessionStatus;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.Ticket;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.model.User;
import vn.edu.fpt.model.Voucher;

// UC-13 Book Ticket Online (và JOB-01): mọi thao tác ghi đều nhận Connection do BookingService mở transaction.
// Quy ước Seat_Hold.SessionID: 'CHECKOUT-<uuid>' khi khách đang chọn bắp nước/voucher,
// 'BOOKING-<BookingID>' sau khi đã tạo đơn Pending (xem sql/UC13_book_ticket_online.sql).
public class BookingDAO {
    public static final String CHECKOUT_HOLD_PREFIX = "CHECKOUT-";
    public static final String BOOKING_HOLD_PREFIX = "BOOKING-";

    // Đơn Pending còn ghế đang giữ (chưa hết hạn) hoặc đơn đã thanh toán - dùng để chặn voucher/điểm dùng hai lần
    private static final String ACTIVE_BOOKING = "(b.Status = ? OR (b.Status = ? AND EXISTS ("
            + "SELECT 1 FROM Seat_Hold h WHERE h.SessionID = ? + CAST(b.BookingID AS VARCHAR(20)) "
            + "AND h.ExpiryTime > GETDATE())))";

    // ===== Suất chiếu, phim, ghế (cùng dữ liệu SeatMapDAO/MovieDAO nhưng đọc trên Connection của request) =====

    public Showtime findShowtimeWithRoom(Connection conn, int showtimeId) throws SQLException {
        String sql = "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.StartTime, s.EndTime, s.BasePrice, s.Status, "
                + "s.TranslationType, r.RoomName, r.Format, r.TotalSeats, r.Status AS RoomStatus "
                + "FROM Showtime s JOIN Room r ON r.RoomID = s.RoomID WHERE s.ShowtimeID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Showtime showtime = new Showtime();
                showtime.setShowtimeId(rs.getInt("ShowtimeID"));
                showtime.setMovieId(rs.getInt("MovieID"));
                showtime.setRoomId(rs.getInt("RoomID"));
                Timestamp start = rs.getTimestamp("StartTime");
                showtime.setStartTime(start == null ? null : start.toLocalDateTime());
                Timestamp end = rs.getTimestamp("EndTime");
                showtime.setEndTime(end == null ? null : end.toLocalDateTime());
                showtime.setBasePrice(rs.getBigDecimal("BasePrice"));
                showtime.setStatus(rs.getString("Status"));
                showtime.setTranslationType(rs.getString("TranslationType"));
                Room room = new Room();
                room.setRoomId(rs.getInt("RoomID"));
                room.setRoomName(rs.getString("RoomName"));
                room.setFormat(rs.getString("Format"));
                room.setTotalSeats(rs.getInt("TotalSeats"));
                room.setStatus(rs.getString("RoomStatus"));
                showtime.setRoom(room);
                return showtime;
            }
        }
    }

    public Movie findMovie(Connection conn, int movieId) throws SQLException {
        String sql = "SELECT MovieID, Title, DurationMinutes, AgeRating, PosterURL, Status FROM Movie WHERE MovieID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Movie movie = new Movie();
                movie.setMovieId(rs.getInt("MovieID"));
                movie.setTitle(rs.getString("Title"));
                movie.setDurationMinutes(rs.getInt("DurationMinutes"));
                movie.setAgeRating(rs.getString("AgeRating"));
                movie.setPosterUrl(rs.getString("PosterURL"));
                movie.setStatus(rs.getString("Status"));
                return movie;
            }
        }
    }

    public Map<Integer, Seat> findRoomSeats(Connection conn, int roomId) throws SQLException {
        String sql = "SELECT SeatID, RoomID, RowName, ColNumber, SeatType, Status FROM Seat WHERE RoomID = ?";
        Map<Integer, Seat> seats = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat seat = new Seat();
                    seat.setSeatId(rs.getInt("SeatID"));
                    seat.setRoomId(rs.getInt("RoomID"));
                    seat.setRowName(rs.getString("RowName"));
                    seat.setColNumber(rs.getInt("ColNumber"));
                    seat.setSeatType(rs.getString("SeatType"));
                    seat.setStatus(rs.getString("Status"));
                    seats.put(seat.getSeatId(), seat);
                }
            }
        }
        return seats;
    }

    // Tên món của đơn (kể cả món đã ẩn/hết hàng sau khi đặt)
    public Map<Integer, String> findConcessionNames(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT c.ConcessionID, c.Name FROM Booking_Concession bc "
                + "JOIN Concession c ON c.ConcessionID = bc.ConcessionID WHERE bc.BookingID = ?";
        Map<Integer, String> names = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.put(rs.getInt("ConcessionID"), rs.getString("Name"));
                }
            }
        }
        return names;
    }

    // ===== Seat_Hold =====

    public int deleteHoldsBySession(Connection conn, String sessionId) throws SQLException {
        String sql = "DELETE FROM Seat_Hold WHERE SessionID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            return ps.executeUpdate();
        }
    }

    // UNIQUE (ShowtimeID, SeatID) nên phải xóa hold hết hạn trước khi người khác giữ lại ghế đó
    public int deleteExpiredHolds(Connection conn, int showtimeId) throws SQLException {
        String sql = "DELETE FROM Seat_Hold WHERE ShowtimeID = ? AND ExpiryTime <= GETDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            return ps.executeUpdate();
        }
    }

    public int deleteAllExpiredHolds(Connection conn) throws SQLException {
        String sql = "DELETE FROM Seat_Hold WHERE ExpiryTime <= GETDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // GB-02: ghế đang bảo trì, đang được giữ, hoặc đã có vé của đơn Paid/Pending thì không giữ được
    public Set<Integer> findUnavailableSeatIds(Connection conn, int showtimeId, List<Integer> seatIds)
            throws SQLException {
        String sql = "SELECT s.SeatID FROM Seat s WHERE s.SeatID IN (" + placeholders(seatIds.size()) + ") "
                + "AND (s.Status = ? "
                + "OR EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.ShowtimeID = ? AND h.SeatID = s.SeatID "
                + "AND h.ExpiryTime > GETDATE()) "
                + "OR EXISTS (SELECT 1 FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "WHERE b.ShowtimeID = ? AND t.SeatID = s.SeatID AND b.Status IN (?, ?) AND t.Status IN (?, ?)))";
        Set<Integer> result = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer seatId : seatIds) {
                ps.setInt(idx++, seatId);
            }
            ps.setString(idx++, RoomStatus.MAINTENANCE);
            ps.setInt(idx++, showtimeId);
            ps.setInt(idx++, showtimeId);
            ps.setString(idx++, BookingStatus.PAID);
            ps.setString(idx++, BookingStatus.PENDING);
            ps.setString(idx++, TicketStatus.UPCOMING);
            ps.setString(idx, TicketStatus.WATCHED);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt("SeatID"));
                }
            }
        }
        return result;
    }

    // ExpiryTime tính theo đồng hồ DB để khớp với mọi truy vấn so sánh GETDATE()
    public void insertHolds(Connection conn, String sessionId, int userId, int showtimeId, List<Integer> seatIds,
                            int holdMinutes) throws SQLException {
        String sql = "INSERT INTO Seat_Hold (SessionID, UserID, ShowtimeID, SeatID, HoldTime, ExpiryTime) "
                + "VALUES (?, ?, ?, ?, GETDATE(), DATEADD(MINUTE, ?, GETDATE()))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer seatId : seatIds) {
                ps.setString(1, sessionId);
                ps.setInt(2, userId);
                ps.setInt(3, showtimeId);
                ps.setInt(4, seatId);
                ps.setInt(5, holdMinutes);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // Ghế của phiên/đơn còn đang giữ; lock = true khóa các dòng này tới hết transaction
    public Set<Integer> findLiveHeldSeatIds(Connection conn, String sessionId, boolean lock) throws SQLException {
        String sql = "SELECT SeatID FROM Seat_Hold " + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "")
                + "WHERE SessionID = ? AND ExpiryTime > GETDATE()";
        Set<Integer> seatIds = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seatIds.add(rs.getInt("SeatID"));
                }
            }
        }
        return seatIds;
    }

    // Số giây còn lại của lượt giữ ghế (0 nếu đã hết hạn) để hiển thị đồng hồ đếm ngược
    public long findRemainingHoldSeconds(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT MIN(DATEDIFF(SECOND, GETDATE(), ExpiryTime)) FROM Seat_Hold "
                + "WHERE SessionID = ? AND ExpiryTime > GETDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(0, rs.getLong(1)) : 0;
            }
        }
    }

    public int renameHoldSession(Connection conn, String fromSessionId, String toSessionId) throws SQLException {
        String sql = "UPDATE Seat_Hold SET SessionID = ? WHERE SessionID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toSessionId);
            ps.setString(2, fromSessionId);
            return ps.executeUpdate();
        }
    }

    // ===== Đơn Pending hết hạn (E1, JOB-01) =====

    // Đơn online Pending không còn ghế nào đang giữ; showtimeId = 0 là quét mọi suất chiếu
    public List<Integer> findExpiredPendingBookingIds(Connection conn, int showtimeId) throws SQLException {
        String sql = "SELECT b.BookingID FROM Booking b WITH (UPDLOCK, ROWLOCK) "
                + "WHERE b.Status = ? AND b.Channel = ? " + (showtimeId > 0 ? "AND b.ShowtimeID = ? " : "")
                + "AND NOT EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.SessionID = ? + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE())";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, BookingStatus.PENDING);
            ps.setString(idx++, BookingChannel.ONLINE);
            if (showtimeId > 0) {
                ps.setInt(idx++, showtimeId);
            }
            ps.setString(idx, BOOKING_HOLD_PREFIX);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("BookingID"));
                }
            }
        }
        return ids;
    }

    // GB-12: không xóa đơn, chỉ chuyển Cancelled; vé của đơn cũng Cancelled để ghế trở lại AVAILABLE.
    // Bắp nước đã giữ kho lúc tạo đơn Pending nên trả lại kho (chỉ khi đơn thực sự chuyển từ Pending sang Cancelled).
    public int cancelPendingBooking(Connection conn, int bookingId) throws SQLException {
        String ticketSql = "UPDATE Ticket SET Status = ? WHERE BookingID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(ticketSql)) {
            ps.setString(1, TicketStatus.CANCELLED);
            ps.setInt(2, bookingId);
            ps.setString(3, TicketStatus.UPCOMING);
            ps.executeUpdate();
        }
        String bookingSql = "UPDATE Booking SET Status = ? WHERE BookingID = ? AND Status = ?";
        int cancelled;
        try (PreparedStatement ps = conn.prepareStatement(bookingSql)) {
            ps.setString(1, BookingStatus.CANCELLED);
            ps.setInt(2, bookingId);
            ps.setString(3, BookingStatus.PENDING);
            cancelled = ps.executeUpdate();
        }
        if (cancelled == 1) {
            restoreStock(conn, bookingId);
        }
        return cancelled;
    }

    // ===== Điểm tích lũy =====

    public Integer findPointBalance(Connection conn, int userId, boolean lock) throws SQLException {
        String sql = "SELECT PointBalance FROM [User] " + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "") + "WHERE UserID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // Điểm đã đặt vào các đơn Pending còn hiệu lực khác của khách (chưa trừ khỏi PointBalance cho tới khi thanh toán)
    public int sumReservedPoints(Connection conn, int customerId, int excludeBookingId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(b.PointsRedeemed), 0) FROM Booking b "
                + "WHERE b.CustomerID = ? AND b.BookingID <> ? AND b.Status = ? AND b.PointsRedeemed > 0 "
                + "AND EXISTS (SELECT 1 FROM Seat_Hold h WHERE h.SessionID = ? + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, excludeBookingId);
            ps.setString(3, BookingStatus.PENDING);
            ps.setString(4, BOOKING_HOLD_PREFIX);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean deductPoints(Connection conn, int userId, int points) throws SQLException {
        String sql = "UPDATE [User] SET PointBalance = PointBalance - ? WHERE UserID = ? AND PointBalance >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, userId);
            ps.setInt(3, points);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean addPoints(Connection conn, int userId, int points) throws SQLException {
        String sql = "UPDATE [User] SET PointBalance = PointBalance + ? "
                + "WHERE UserID = ? AND PointBalance <= 2147483647 - ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setInt(2, userId);
            ps.setInt(3, points);
            return ps.executeUpdate() == 1;
        }
    }

    public void insertLoyaltyTransaction(Connection conn, int userId, int bookingId, String type, int points,
                                         String description) throws SQLException {
        String sql = "INSERT INTO LoyaltyPointHistory "
                + "(UserID, BookingID, TransactionType, PointsChange, Description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, bookingId);
            ps.setString(3, type);
            ps.setInt(4, points);
            ps.setString(5, description);
            ps.executeUpdate();
        }
    }

    // ===== Voucher =====

    // Trạng thái hiệu lực giống VoucherDAO: Active nhưng quá hạn thì coi là Expired, chưa tới StartDate thì Scheduled
    private static final String VOUCHER_COLUMNS = "SELECT VoucherID, VoucherCode, DiscountValue, DiscountType, "
            + "StartDate, ExpiryDate, UsageLimit, OwnerUserID, "
            + "CASE WHEN Status = 'Active' AND ExpiryDate <= GETDATE() THEN 'Expired' "
            + "WHEN Status = 'Active' AND StartDate > GETDATE() THEN 'Scheduled' ELSE Status END AS Status, "
            + "VoucherType, MinOrderAmount FROM Voucher ";

    public Voucher findVoucherByCode(Connection conn, String voucherCode) throws SQLException {
        // Khóa ghi voucher trong lúc đọc để tránh race condition khi nhiều booking cùng dùng 1 voucher
        try (PreparedStatement ps = conn.prepareStatement(
                VOUCHER_COLUMNS + "WITH (UPDLOCK, HOLDLOCK) WHERE VoucherCode = ?")) {
            ps.setString(1, voucherCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapVoucher(rs) : null;
            }
        }
    }

    // Voucher còn hiệu lực trong ví của khách (voucher cá nhân, kể cả hoàn vé 80%), hạn gần nhất lên trước
    public List<Voucher> findActiveVouchersOfOwner(Connection conn, int ownerUserId) throws SQLException {
        String sql = VOUCHER_COLUMNS + "WHERE OwnerUserID = ? AND Status = 'Active' AND ExpiryDate > GETDATE() "
                + "ORDER BY ExpiryDate, VoucherID";
        List<Voucher> vouchers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vouchers.add(mapVoucher(rs));
                }
            }
        }
        return vouchers;
    }

    private Voucher mapVoucher(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(rs.getInt("VoucherID"));
        voucher.setVoucherCode(rs.getString("VoucherCode"));
        voucher.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        voucher.setDiscountType(rs.getString("DiscountType"));
        Timestamp start = rs.getTimestamp("StartDate");
        voucher.setStartDate(start == null ? null : start.toLocalDateTime());
        Timestamp expiry = rs.getTimestamp("ExpiryDate");
        voucher.setExpiryDate(expiry == null ? null : expiry.toLocalDateTime());
        voucher.setUsageLimit(rs.getObject("UsageLimit", Integer.class));
        voucher.setOwnerUserId(rs.getObject("OwnerUserID", Integer.class));
        voucher.setStatus(rs.getString("Status"));
        voucher.setVoucherType(rs.getString("VoucherType"));
        BigDecimal minOrder = rs.getBigDecimal("MinOrderAmount");
        voucher.setMinOrderAmount(minOrder == null ? BigDecimal.ZERO : minOrder);
        return voucher;
    }

    // Voucher đã nằm trong đơn Paid hoặc đơn Pending còn hiệu lực; customerId != null thì chỉ xét đơn của khách đó
    public boolean isVoucherInUse(Connection conn, int voucherId, Integer customerId, int excludeBookingId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM Booking b WHERE b.VoucherID = ? AND b.BookingID <> ? "
                + (customerId != null ? "AND b.CustomerID = ? " : "") + "AND " + ACTIVE_BOOKING;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, voucherId);
            ps.setInt(idx++, excludeBookingId);
            if (customerId != null) {
                ps.setInt(idx++, customerId);
            }

            ps.setString(idx++, BookingStatus.PAID);
            ps.setString(idx++, BookingStatus.PENDING);
            ps.setString(idx, BOOKING_HOLD_PREFIX);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int countActiveVoucherUses(Connection conn, int voucherId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Booking b WHERE b.VoucherID = ? AND " + ACTIVE_BOOKING;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setString(2, BookingStatus.PAID);
            ps.setString(3, BookingStatus.PENDING);
            ps.setString(4, BOOKING_HOLD_PREFIX);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Voucher cá nhân dùng một lần; voucher công khai (OwnerUserID NULL) không đổi trạng thái
    public boolean markPersonalVoucherUsed(Connection conn, int voucherId) throws SQLException {
        String sql = "UPDATE Voucher SET Status = 'Used' "
                + "WHERE VoucherID = ? AND Status = 'Active' AND OwnerUserID IS NOT NULL AND ExpiryDate > GETDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            return ps.executeUpdate() == 1;
        }
    }

    public String findVoucherCode(Connection conn, int voucherId) throws SQLException {
        String sql = "SELECT VoucherCode FROM Voucher WHERE VoucherID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ===== Bắp nước =====

    // Món đang bán và còn hàng, đọc trong transaction để kiểm tra tồn kho lúc chốt đơn
    public List<Concession> findConcessionsForSale(Connection conn) throws SQLException {
        String sql = "SELECT ConcessionID, Name, Description, Price, StockQuantity, ImageUrl, Status FROM Concession "
                + "WHERE Status = ? AND StockQuantity > 0 ORDER BY Name";
        List<Concession> concessions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ConcessionStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Concession concession = new Concession();
                    concession.setConcessionId(rs.getInt("ConcessionID"));
                    concession.setName(rs.getString("Name"));
                    concession.setDescription(rs.getString("Description"));
                    concession.setPrice(rs.getBigDecimal("Price"));
                    concession.setStockQuantity(rs.getInt("StockQuantity"));
                    concession.setImageUrl(rs.getString("ImageUrl"));
                    concession.setStatus(rs.getString("Status"));
                    concessions.add(concession);
                }
            }
        }
        return concessions;
    }

    // GB-10: trừ kho (đơn online: lúc tạo đơn Pending; tại quầy: lúc xác nhận); về 0 thì chuyển Out of Stock
    public boolean decreaseStock(Connection conn, int concessionId, int quantity) throws SQLException {
        String sql = "UPDATE Concession SET StockQuantity = StockQuantity - ?, "
                + "Status = CASE WHEN StockQuantity - ? = 0 THEN ? ELSE Status END "
                + "WHERE ConcessionID = ? AND StockQuantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, quantity);
            ps.setString(3, ConcessionStatus.OUT_OF_STOCK);
            ps.setInt(4, concessionId);
            ps.setInt(5, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    // Trả lại kho bắp nước của đơn bị hủy; món đang Out of Stock có hàng trở lại thì Active (món Hidden giữ nguyên)
    private void restoreStock(Connection conn, int bookingId) throws SQLException {
        String sql = "UPDATE c SET c.StockQuantity = c.StockQuantity + bc.Quantity, "
                + "c.Status = CASE WHEN c.Status = ? THEN ? ELSE c.Status END "
                + "FROM Concession c JOIN Booking_Concession bc ON bc.ConcessionID = c.ConcessionID "
                + "WHERE bc.BookingID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ConcessionStatus.OUT_OF_STOCK);
            ps.setString(2, ConcessionStatus.ACTIVE);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        }
    }

    // ===== Email xác nhận vé =====

    // Chỉ điền UserID, Email, FullName của khách
    public User findCustomerContact(Connection conn, int userId) throws SQLException {
        String sql = "SELECT UserID, Email, FullName FROM [User] WHERE UserID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = new User();
                user.setUserId(rs.getInt("UserID"));
                user.setEmail(rs.getString("Email"));
                user.setFullName(rs.getString("FullName"));
                return user;
            }
        }
    }

    // ===== Booking / Ticket =====

    public int insertBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO Booking (CustomerID, StaffID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, "
                + "FinalAmount, PaymentMethod, BookingTime, Status, Channel, PointsRedeemed, PointsEarned) "
                + "VALUES (?, NULL, ?, ?, ?, ?, ?, NULL, GETDATE(), ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, booking.getCustomerId());
            ps.setInt(2, booking.getShowtimeId());
            if (booking.getVoucherId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, booking.getVoucherId());
            }
            ps.setBigDecimal(4, booking.getTotalPrice());
            ps.setBigDecimal(5, booking.getDiscountAmount());
            ps.setBigDecimal(6, booking.getFinalAmount());
            ps.setString(7, booking.getStatus());
            ps.setString(8, booking.getChannel());
            ps.setInt(9, booking.getPointsRedeemed());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được BookingID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    public void insertTickets(Connection conn, List<Ticket> tickets) throws SQLException {
        String sql = "INSERT INTO Ticket (BookingID, SeatID, Price, TicketCode, QRCode, Status, ShowtimeID) "
                + "VALUES (?, ?, ?, ?, NULL, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Ticket ticket : tickets) {
                ps.setInt(1, ticket.getBookingId());
                ps.setInt(2, ticket.getSeatId());
                ps.setBigDecimal(3, ticket.getPrice());
                ps.setString(4, ticket.getTicketCode());
                ps.setString(5, ticket.getStatus());
                ps.setInt(6, ticket.getShowtimeId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void insertBookingConcessions(Connection conn, List<BookingConcession> items) throws SQLException {
        if (items.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO Booking_Concession (BookingID, ConcessionID, Quantity, SubtotalPrice) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (BookingConcession item : items) {
                ps.setInt(1, item.getBookingId());
                ps.setInt(2, item.getConcessionId());
                ps.setInt(3, item.getQuantity());
                ps.setBigDecimal(4, item.getSubtotalPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Booking findBookingOfCustomer(Connection conn, int bookingId, int customerId, boolean lock)
            throws SQLException {
        String sql = "SELECT BookingID, CustomerID, ShowtimeID, VoucherID, TotalPrice, DiscountAmount, FinalAmount, "
                + "PaymentMethod, BookingTime, Status, Channel, PointsRedeemed, PointsEarned FROM Booking "
                + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "") + "WHERE BookingID = ? AND CustomerID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Booking booking = new Booking();
                booking.setBookingId(rs.getInt("BookingID"));
                booking.setCustomerId(rs.getObject("CustomerID", Integer.class));
                booking.setShowtimeId(rs.getInt("ShowtimeID"));
                booking.setVoucherId(rs.getObject("VoucherID", Integer.class));
                booking.setTotalPrice(rs.getBigDecimal("TotalPrice"));
                booking.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                booking.setFinalAmount(rs.getBigDecimal("FinalAmount"));
                booking.setPaymentMethod(rs.getString("PaymentMethod"));
                Timestamp time = rs.getTimestamp("BookingTime");
                booking.setBookingTime(time == null ? null : time.toLocalDateTime());
                booking.setStatus(rs.getString("Status"));
                booking.setChannel(rs.getString("Channel"));
                booking.setPointsRedeemed(rs.getInt("PointsRedeemed"));
                booking.setPointsEarned(rs.getInt("PointsEarned"));
                return booking;
            }
        }
    }

    public List<Ticket> findTickets(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT TicketID, BookingID, SeatID, Price, TicketCode, QRCode, Status, ShowtimeID "
                + "FROM Ticket WHERE BookingID = ? ORDER BY TicketID";
        List<Ticket> tickets = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket ticket = new Ticket();
                    ticket.setTicketId(rs.getInt("TicketID"));
                    ticket.setBookingId(rs.getInt("BookingID"));
                    ticket.setSeatId(rs.getInt("SeatID"));
                    ticket.setPrice(rs.getBigDecimal("Price"));
                    ticket.setTicketCode(rs.getString("TicketCode"));
                    ticket.setQrCode(rs.getString("QRCode"));
                    ticket.setStatus(rs.getString("Status"));
                    ticket.setShowtimeId(rs.getObject("ShowtimeID", Integer.class));
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    public List<BookingConcession> findBookingConcessions(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT BookingID, ConcessionID, Quantity, SubtotalPrice FROM Booking_Concession "
                + "WHERE BookingID = ? ORDER BY ConcessionID";
        List<BookingConcession> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingConcession item = new BookingConcession();
                    item.setBookingId(rs.getInt("BookingID"));
                    item.setConcessionId(rs.getInt("ConcessionID"));
                    item.setQuantity(rs.getInt("Quantity"));
                    item.setSubtotalPrice(rs.getBigDecimal("SubtotalPrice"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public void updateTicketQrCodes(Connection conn, List<Ticket> tickets) throws SQLException {
        String sql = "UPDATE Ticket SET QRCode = ? WHERE TicketID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Ticket ticket : tickets) {
                ps.setString(1, ticket.getQrCode());
                ps.setInt(2, ticket.getTicketId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public boolean setBookingPaymentMethod(Connection conn, int bookingId, String paymentMethod) throws SQLException {
        String sql = "UPDATE Booking SET PaymentMethod = ? WHERE BookingID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentMethod);
            ps.setInt(2, bookingId);
            ps.setString(3, BookingStatus.PENDING);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean markBookingPaid(Connection conn, int bookingId, String paymentMethod, int pointsEarned)
            throws SQLException {
        String sql = "UPDATE Booking SET Status = ?, PaymentMethod = ?, PointsEarned = ? "
                + "WHERE BookingID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BookingStatus.PAID);
            ps.setString(2, paymentMethod);
            ps.setInt(3, pointsEarned);
            ps.setInt(4, bookingId);
            ps.setString(5, BookingStatus.PENDING);
            return ps.executeUpdate() == 1;
        }
    }

    private String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }
}
