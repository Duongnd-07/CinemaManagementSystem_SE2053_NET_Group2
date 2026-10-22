package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.dao.BookingDAO;
import vn.edu.fpt.dao.PaymentDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.model.BookingConcession;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.PaymentAttempt;
import vn.edu.fpt.model.PaymentMethod;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatType;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.Ticket;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.model.User;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.service.payment.PaymentGateway;
import vn.edu.fpt.service.payment.PaymentGatewayException;
import vn.edu.fpt.service.payment.PaymentRequest;
import vn.edu.fpt.service.payment.PaymentSession;
import vn.edu.fpt.service.payment.VerifiedPayment;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-13: Book Ticket Online.
// Bước 1-4 chọn ghế (chỉ đọc, dùng dữ liệu UC-09) -> bước 5 giữ ghế 10 phút (Seat_Hold) -> bước 6-8 bắp nước,
// voucher, điểm (lưu trong BookingCheckout) -> bước 9 tạo đơn Pending -> thanh toán qua UC-14.
// Mỗi thao tác chỉ mở MỘT kết nối DB và truyền xuống DAO: trang checkout tự tính lại tiền mỗi lần khách đổi lựa
// chọn, mở kết nối mới cho từng truy vấn sẽ làm mỗi lần tính chậm vài giây.
public class BookingService {
    private static final Logger LOGGER = Logger.getLogger(BookingService.class.getName());
    // Khóa lỗi khiến phiên đặt vé kết thúc (E1 hết giờ giữ ghế, suất chiếu không còn mở bán...): quay lại chọn ghế
    public static final String ERROR_ENDED = "ended";
    public static final String ERROR_GENERAL = "general";
    public static final String ERROR_VOUCHER = "voucher";
    public static final String ERROR_POINTS = "points";
    public static final String ERROR_CONCESSION = "concession";

    public static final int MAX_CONCESSION_QUANTITY = 10;

    // BR-AGE-01: phim cần xác nhận độ tuổi trước khi đặt vé
    private static final List<String> AGE_RESTRICTED = List.of("C13", "C16", "C18");
    private static final int DEFAULT_HOLD_MINUTES = 10;
    private static final int DEFAULT_POINT_RATE = 1000;
    private static final int DEFAULT_MAX_SEATS = 8;
    // Ghế VIP mặc định đắt hơn ghế thường 30.000đ khi SystemConfig chưa có SEAT_SURCHARGE_VIP
    private static final BigDecimal DEFAULT_VIP_SURCHARGE = BigDecimal.valueOf(30000);
    private static final String CONFIG_HOLD_MINUTES = "SEAT_HOLD_MINUTES";
    private static final String CONFIG_POINT_RATE = "POINT_EXCHANGE_RATE";
    private static final String CONFIG_MAX_SEATS = "BOOKING_MAX_SEATS";
    private static final String CONFIG_VIP_SURCHARGE = "SEAT_SURCHARGE_VIP";
    // SQL Server: vi phạm UNIQUE index / constraint
    private static final Set<Integer> DUPLICATE_KEY_ERRORS = Set.of(2601, 2627);
    private static final String TICKET_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TICKET_CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SeatMapService seatMapService = new SeatMapService();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();

    // ===== Bước 1-4: chọn ghế =====

    public BookingSeatView loadSeatSelection(int showtimeId) throws SQLException, ValidationException {
        SeatMapView seatMap = seatMapService.load(showtimeId);
        try (Connection conn = DBContext.getConnection()) {
            return new BookingSeatView(seatMap, seatPrices(conn, seatMap.getShowtime().getBasePrice()),
                    maxSeats(conn), isAgeRestricted(seatMap.getMovie()));
        }
    }

    // ===== Bước 5: giữ ghế =====

    // previous: phiên đặt vé trước đó của cùng trình duyệt (nếu có) - nhả ghế cũ khi khách chọn lại
    public BookingCheckout holdSeats(User customer, int showtimeId, List<Integer> requestedSeatIds,
                                     boolean ageConfirmed, BookingCheckout previous)
            throws SQLException, ValidationException {
        requireCustomer(customer);
        List<Integer> seatIds = new ArrayList<>(new LinkedHashSet<>(requestedSeatIds));
        seatIds.removeIf(id -> id == null || id <= 0);
        if (seatIds.isEmpty()) {
            throw error(ERROR_GENERAL, Messages.BOOKING_SEAT_REQUIRED);
        }
        String holdSessionId = BookingDAO.CHECKOUT_HOLD_PREFIX + UUID.randomUUID();
        try {
            inTransaction(conn -> {
                int maxSeats = maxSeats(conn);
                if (seatIds.size() > maxSeats) {
                    throw error(ERROR_GENERAL, Messages.format(Messages.BOOKING_SEAT_LIMIT, maxSeats));
                }
                ShowtimeInfo info = loadBookableShowtime(conn, showtimeId);
                if (isAgeRestricted(info.movie) && !ageConfirmed) {
                    throw error(ERROR_GENERAL,
                            Messages.format(Messages.BOOKING_AGE_REQUIRED, info.movie.getAgeRating()));
                }
                if (!bookingDAO.findRoomSeats(conn, info.room.getRoomId()).keySet().containsAll(seatIds)) {
                    throw error(ERROR_GENERAL, Messages.BOOKING_SEAT_INVALID);
                }
                if (previous != null && previous.getBookingId() == null) {
                    bookingDAO.deleteHoldsBySession(conn, previous.getHoldSessionId());
                }
                cancelExpiredBookings(conn, showtimeId);
                bookingDAO.deleteExpiredHolds(conn, showtimeId);
                // E2: ghế vừa bị người khác giữ/mua hoặc bị khóa bảo trì sau khi khách mở sơ đồ
                if (!bookingDAO.findUnavailableSeatIds(conn, showtimeId, seatIds).isEmpty()) {
                    throw error(ERROR_GENERAL, Messages.BOOKING_SEAT_TAKEN);
                }
                bookingDAO.insertHolds(conn, holdSessionId, customer.getUserId(), showtimeId, seatIds,
                        holdMinutes(conn));
                return null;
            });
        } catch (SQLException e) {
            // GB-01 mutual exclusion: hai phiên chèn cùng (ShowtimeID, SeatID) cùng lúc, UNIQUE chặn phiên đến sau
            if (isDuplicateKey(e)) {
                throw error(ERROR_GENERAL, Messages.BOOKING_SEAT_TAKEN);
            }
            throw e;
        }
        return new BookingCheckout(holdSessionId, showtimeId, seatIds);
    }

    // ===== Bước 6-8: bắp nước, voucher, điểm =====

    public CheckoutView loadCheckout(User customer, BookingCheckout checkout)
            throws SQLException, ValidationException {
        requireCustomer(customer);
        try (Connection conn = DBContext.getConnection()) {
            CheckoutContext context = loadContext(conn, checkout, false);
            Map<Integer, Concession> forSale = concessionsForSale(conn);
            Map<String, String> errors = new LinkedHashMap<>();
            // Món đã chọn nhưng vừa ngừng bán/hết hàng thì bỏ khỏi đơn và báo cho khách
            Map<Integer, Integer> picked = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> entry : checkout.getConcessions().entrySet()) {
                Concession concession = forSale.get(entry.getKey());
                if (concession == null || entry.getValue() > maxQuantity(concession)) {
                    errors.put(ERROR_CONCESSION, Messages.format(Messages.BOOKING_CONCESSION_UNAVAILABLE,
                            concession == null ? "đã chọn" : concession.getName()));
                } else {
                    picked.put(entry.getKey(), entry.getValue());
                }
            }
            BookingQuote quote = quote(conn, customer, context.ticketTotal, picked, forSale,
                    checkout.getVoucherCode(), checkout.getPointsRedeemed(), errors);
            remember(checkout, picked, quote);
            return checkoutView(conn, customer, context, forSale, picked, quote, errors);
        }
    }

    // Lưu lựa chọn hợp lệ vào phiên đặt vé rồi trả về trang đã tính lại; phần sai bị bỏ
    // (E3: tính lại tổng không có giảm giá) và lỗi nằm trong CheckoutView.getErrors()
    public CheckoutView applyOptions(User customer, BookingCheckout checkout, CheckoutForm form)
            throws SQLException, ValidationException {
        requireCustomer(customer);
        try (Connection conn = DBContext.getConnection()) {
            CheckoutContext context = loadContext(conn, checkout, false);
            Applied applied = apply(conn, customer, checkout, context, form);
            return checkoutView(conn, customer, context, applied.forSale, checkout.getConcessions(), applied.quote,
                    applied.errors);
        }
    }

    // ===== Bước 9: tạo đơn Pending =====

    public int confirm(User customer, BookingCheckout checkout, CheckoutForm form)
            throws SQLException, ValidationException {
        requireCustomer(customer);
        int bookingId;
        try {
            bookingId = inTransaction(conn -> {
                CheckoutContext context = loadContext(conn, checkout, true);
                // Khóa tài khoản để hai đơn của cùng khách không cùng dùng một lượng điểm
                bookingDAO.findPointBalance(conn, customer.getUserId(), true);
                Applied applied = apply(conn, customer, checkout, context, form);
                if (!applied.errors.isEmpty()) {
                    throw new ValidationException(applied.errors);
                }
                BookingQuote quote = applied.quote;

                Booking booking = new Booking();
                booking.setCustomerId(customer.getUserId());
                booking.setShowtimeId(checkout.getShowtimeId());
                booking.setVoucherId(quote.getVoucherId());
                booking.setTotalPrice(quote.getGrossTotal());
                booking.setDiscountAmount(quote.getDiscountTotal());
                booking.setFinalAmount(quote.getFinalAmount());
                booking.setStatus(BookingStatus.PENDING);
                booking.setChannel(BookingChannel.ONLINE);
                booking.setPointsRedeemed(quote.getPointsRedeemed());
                int id = bookingDAO.insertBooking(conn, booking);

                List<Ticket> tickets = new ArrayList<>();
                for (BookingSeatLine seat : context.seats) {
                    Ticket ticket = new Ticket();
                    ticket.setBookingId(id);
                    ticket.setSeatId(seat.getSeatId());
                    ticket.setPrice(seat.getPrice());
                    ticket.setTicketCode(newTicketCode());
                    ticket.setStatus(TicketStatus.UPCOMING);
                    ticket.setShowtimeId(checkout.getShowtimeId());
                    tickets.add(ticket);
                }
                bookingDAO.insertTickets(conn, tickets);

                List<BookingConcession> items = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : checkout.getConcessions().entrySet()) {
                    BookingConcession item = new BookingConcession();
                    item.setBookingId(id);
                    item.setConcessionId(entry.getKey());
                    item.setQuantity(entry.getValue());
                    item.setSubtotalPrice(applied.forSale.get(entry.getKey()).getPrice()
                            .multiply(BigDecimal.valueOf(entry.getValue())));
                    items.add(item);
                }
                bookingDAO.insertBookingConcessions(conn, items);
                // Ghế tiếp tục được giữ tới đúng hạn cũ, giờ thuộc về đơn (UC-14 dùng để biết đơn còn hiệu lực)
                bookingDAO.renameHoldSession(conn, checkout.getHoldSessionId(), bookingHoldSession(id));
                return id;
            });
        } catch (SQLException e) {
            // Vé trùng ghế đã có vé còn hiệu lực (UX_Ticket_Showtime_Seat)
            if (isDuplicateKey(e)) {
                throw error(ERROR_ENDED, Messages.BOOKING_SEAT_TAKEN);
            }
            throw e;
        }
        checkout.setBookingId(bookingId);
        return bookingId;
    }

    // AF2 khi đang ở trang checkout, hoặc dọn dẹp khi phiên đặt vé kết thúc: nhả ghế về AVAILABLE ngay
    public void releaseCheckout(BookingCheckout checkout) throws SQLException {
        if (checkout == null || checkout.getBookingId() != null) {
            return;
        }
        try (Connection conn = DBContext.getConnection()) {
            bookingDAO.deleteHoldsBySession(conn, checkout.getHoldSessionId());
        }
    }

    // ===== Sau bước 9: đơn chờ thanh toán =====

    public BookingSummary loadBooking(User customer, int bookingId) throws SQLException, ValidationException {
        requireCustomer(customer);
        return inTransaction(conn -> {
            expireIfNeeded(conn, customer, bookingId);
            Booking booking = bookingDAO.findBookingOfCustomer(conn, bookingId, customer.getUserId(), false);
            if (booking == null) {
                throw error(ERROR_GENERAL, Messages.BOOKING_NOT_FOUND);
            }
            Showtime showtime = bookingDAO.findShowtimeWithRoom(conn, booking.getShowtimeId());
            Room room = showtime.getRoom();
            Movie movie = bookingDAO.findMovie(conn, showtime.getMovieId());
            Map<Integer, Seat> roomSeats = bookingDAO.findRoomSeats(conn, room.getRoomId());
            List<BookingSeatLine> seats = new ArrayList<>();
            for (Ticket ticket : bookingDAO.findTickets(conn, bookingId)) {
                Seat seat = roomSeats.get(ticket.getSeatId());
                seats.add(new BookingSeatLine(ticket.getSeatId(),
                        seat == null ? "#" + ticket.getSeatId() : seat.getRowName() + seat.getColNumber(),
                        seat == null ? SeatType.STANDARD : seat.getSeatType(), ticket.getPrice(),
                        ticket.getTicketCode(), ticket.getQrCode()));
            }
            Map<Integer, String> names = bookingDAO.findConcessionNames(conn, bookingId);
            List<BookingConcessionLine> concessions = new ArrayList<>();
            for (BookingConcession item : bookingDAO.findBookingConcessions(conn, bookingId)) {
                BigDecimal unitPrice = item.getSubtotalPrice()
                        .divide(BigDecimal.valueOf(item.getQuantity()), 0, RoundingMode.HALF_UP);
                concessions.add(new BookingConcessionLine(item.getConcessionId(),
                        names.getOrDefault(item.getConcessionId(), "#" + item.getConcessionId()), null, null,
                        unitPrice, item.getQuantity(), item.getQuantity()));
            }
            String voucherCode = booking.getVoucherId() == null ? null
                    : bookingDAO.findVoucherCode(conn, booking.getVoucherId());
            long remaining = BookingStatus.PENDING.equals(booking.getStatus())
                    ? bookingDAO.findRemainingHoldSeconds(conn, bookingHoldSession(bookingId)) : 0;
            Integer pointBalance = bookingDAO.findPointBalance(conn, customer.getUserId(), false);
            return new BookingSummary(booking, showtime, movie, room, seats, concessions, voucherCode, remaining,
                    paymentDAO.findLatestStatusForBooking(conn, bookingId), pointBalance == null ? 0 : pointBalance);
        });
    }

    public PaymentSession startVnPayPayment(User customer, int bookingId, String clientIpAddress,
                                            String returnUrl, String callbackUrl, PaymentGateway gateway)
            throws SQLException, ValidationException, PaymentGatewayException {
        requireCustomer(customer);
        inTransaction(conn -> {
            expireIfNeeded(conn, customer, bookingId);
            return null;
        });
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Booking booking = lockPendingBooking(conn, customer, bookingId);
                List<Ticket> tickets = bookingDAO.findTickets(conn, bookingId);
                Set<Integer> held = bookingDAO.findLiveHeldSeatIds(conn, bookingHoldSession(bookingId), true);
                if (!held.equals(ticketSeatIds(tickets))) {
                    throw error(ERROR_ENDED, Messages.BOOKING_HOLD_EXPIRED);
                }
                long remainingSeconds = bookingDAO.findRemainingHoldSeconds(conn, bookingHoldSession(bookingId));
                if (remainingSeconds <= 0) {
                    throw error(ERROR_ENDED, Messages.BOOKING_HOLD_EXPIRED);
                }
                if (booking.getFinalAmount() == null || booking.getFinalAmount().signum() <= 0) {
                    throw error(ERROR_GENERAL, Messages.BOOKING_VNPAY_AMOUNT_INVALID);
                }

                String transactionReference = bookingId + "-" + UUID.randomUUID().toString().replace("-", "");
                PaymentRequest paymentRequest = new PaymentRequest();
                paymentRequest.setBookingId(bookingId);
                paymentRequest.setCustomerId(customer.getUserId());
                paymentRequest.setAmount(booking.getFinalAmount());
                paymentRequest.setTransactionReference(transactionReference);
                paymentRequest.setClientIpAddress(clientIpAddress);
                paymentRequest.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .plusSeconds(remainingSeconds));
                paymentRequest.setReturnUrl(returnUrl);
                paymentRequest.setCallbackUrl(callbackUrl);
                PaymentSession paymentSession = gateway.createPayment(paymentRequest);

                paymentDAO.expireInitiatedForBooking(conn, bookingId);
                paymentDAO.insertInitiated(conn, bookingId, transactionReference, booking.getFinalAmount());
                if (!bookingDAO.setBookingPaymentMethod(conn, bookingId, PaymentMethod.VNPAY)) {
                    throw new SQLException("Booking status changed before VNPay session was created.");
                }
                conn.commit();
                return paymentSession;
            } catch (SQLException | ValidationException | PaymentGatewayException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public Integer findBookingIdForPaymentReturn(User customer, String transactionReference) throws SQLException,
            ValidationException {
        requireCustomer(customer);
        try (Connection conn = DBContext.getConnection()) {
            PaymentAttempt attempt = paymentDAO.findByReference(conn, transactionReference, false);
            return attempt != null && attempt.getCustomerId() == customer.getUserId()
                    ? attempt.getBookingId() : null;
        }
    }

    public boolean applyVerifiedVnPayPayment(VerifiedPayment payment) throws SQLException, ValidationException {
        if (payment == null || payment.getTransactionReference() == null) {
            return false;
        }
        PaymentAttempt initialAttempt;
        try (Connection conn = DBContext.getConnection()) {
            initialAttempt = paymentDAO.findByReference(conn, payment.getTransactionReference(), false);
        }
        if (initialAttempt == null) {
            return false;
        }
        try {
            return inTransaction(conn -> {
                Booking booking = bookingDAO.findBookingOfCustomer(conn, initialAttempt.getBookingId(),
                        initialAttempt.getCustomerId(), true);
                PaymentAttempt attempt = paymentDAO.findByReference(conn, payment.getTransactionReference(), true);
                if (attempt == null || attempt.getBookingId() != initialAttempt.getBookingId()) {
                    return false;
                }
                if (PaymentAttempt.STATUS_PAID.equals(attempt.getStatus())) {
                    return payment.isSuccessful() && paymentDAO.hasMatchingPaidTransaction(conn, attempt,
                            payment.getProviderTransactionId(), payment.getPaidAmount());
                }
                if (PaymentAttempt.STATUS_REVIEW_REQUIRED.equals(attempt.getStatus())) {
                    return payment.isSuccessful() && paymentDAO.hasMatchingReviewCallback(attempt,
                            payment.getProviderTransactionId(), payment.getResponseCode(), payment.getPaidAmount());
                }
                if (PaymentAttempt.STATUS_FAILED.equals(attempt.getStatus())) {
                    if (!payment.isSuccessful()) {
                        return paymentDAO.hasMatchingFailedCallback(attempt, payment.getProviderTransactionId(),
                                payment.getResponseCode(), payment.getPaidAmount());
                    }
                    return markVnPayPaymentForReview(conn, payment, "success arrived after a failed callback");
                }
                if (PaymentAttempt.STATUS_EXPIRED.equals(attempt.getStatus())) {
                    return payment.isSuccessful()
                            ? markVnPayPaymentForReview(conn, payment, "success arrived after the attempt expired")
                            : true;
                }
                if (!PaymentAttempt.STATUS_INITIATED.equals(attempt.getStatus())) {
                    return false;
                }
                if (!payment.isSuccessful()) {
                    paymentDAO.markFailed(conn, payment.getTransactionReference(), payment.getProviderTransactionId(),
                            payment.getPaidAmount(), payment.getResponseCode());
                    return true;
                }
                if (payment.getPaidAmount() == null || payment.getPaidAt() == null
                        || payment.getProviderTransactionId() == null || payment.getProviderTransactionId().isBlank()
                        || attempt.getAmount().compareTo(payment.getPaidAmount()) != 0
                        || booking == null || !BookingStatus.PENDING.equals(booking.getStatus())
                        || booking.getFinalAmount().compareTo(payment.getPaidAmount()) != 0) {
                    return markVnPayPaymentForReview(conn, payment,
                            "successful callback did not match the pending booking amount or status");
                }

                completeBookingPayment(conn, booking, attempt.getCustomerId(), PaymentMethod.VNPAY);
                if (!paymentDAO.markPaid(conn, payment.getTransactionReference(), payment.getProviderTransactionId(),
                        payment.getPaidAmount(), payment.getResponseCode(), payment.getPaidAt())) {
                    throw new SQLException("Could not record verified VNPay transaction.");
                }
                return true;
            });
        } catch (ValidationException e) {
            if (!payment.isSuccessful()) {
                throw e;
            }
            boolean recordedForReview = inTransaction(conn -> markVnPayPaymentForReview(conn, payment,
                    "booking settlement prerequisites failed: " + e.getMessage()));
            return recordedForReview;
        }
    }

    // Đơn 0đ (voucher/điểm trừ hết tiền): VNPay không nhận số tiền 0 nên chốt đơn ngay, cùng các bước như khi
    // VNPay báo thành công (trừ kho, khóa voucher cá nhân, trừ điểm, sinh QR)
    public void confirmFreeBooking(User customer, int bookingId) throws SQLException, ValidationException {
        requireCustomer(customer);
        inTransaction(conn -> {
            expireIfNeeded(conn, customer, bookingId);
            return null;
        });
        inTransaction(conn -> {
            Booking booking = lockPendingBooking(conn, customer, bookingId);
            if (booking.getFinalAmount() == null || booking.getFinalAmount().signum() != 0) {
                throw error(ERROR_GENERAL, Messages.BOOKING_NOT_FREE);
            }
            completeBookingPayment(conn, booking, customer.getUserId(), null);
            return null;
        });
    }

    // AF2 khi đơn đã tạo nhưng chưa thanh toán: đơn và vé chuyển Cancelled, ghế về AVAILABLE ngay
    public void cancelBooking(User customer, int bookingId) throws SQLException, ValidationException {
        requireCustomer(customer);
        inTransaction(conn -> {
            lockPendingBooking(conn, customer, bookingId);
            bookingDAO.cancelPendingBooking(conn, bookingId);
            bookingDAO.deleteHoldsBySession(conn, bookingHoldSession(bookingId));
            return null;
        });
    }

    // ===== JOB-01 =====

    // Hủy đơn online Pending đã hết giờ giữ ghế và xóa mọi Seat_Hold hết hạn. Trả về số đơn đã hủy.
    public int cancelAllExpiredBookings() throws SQLException {
        try {
            return inTransaction(conn -> cancelExpiredBookings(conn, 0));
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    public int releaseAllExpiredHolds() throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return bookingDAO.deleteAllExpiredHolds(conn);
        }
    }

    // ===== Nội bộ =====

    private boolean markVnPayPaymentForReview(Connection conn, VerifiedPayment payment, String reason)
            throws SQLException {
        PaymentAttempt attempt = paymentDAO.findByReference(conn, payment.getTransactionReference(), true);
        if (attempt == null) {
            return false;
        }
        if (PaymentAttempt.STATUS_PAID.equals(attempt.getStatus())) {
            return paymentDAO.hasMatchingPaidTransaction(conn, attempt, payment.getProviderTransactionId(),
                    payment.getPaidAmount());
        }
        if (PaymentAttempt.STATUS_REVIEW_REQUIRED.equals(attempt.getStatus())) {
            return paymentDAO.hasMatchingReviewCallback(attempt, payment.getProviderTransactionId(),
                    payment.getResponseCode(), payment.getPaidAmount());
        }
        boolean recorded = paymentDAO.markReviewRequired(conn, payment.getTransactionReference(),
                payment.getProviderTransactionId(), payment.getPaidAmount(), payment.getResponseCode());
        if (recorded) {
            LOGGER.log(Level.SEVERE,
                    "Verified VNPay payment requires manual review. reference={0}; reason={1}",
                    new Object[]{payment.getTransactionReference(), reason});
        }
        return recorded;
    }

    // paymentMethod = null khi đơn 0đ: không có giao dịch tiền nên không ghi phương thức thanh toán
    private void completeBookingPayment(Connection conn, Booking booking, int customerId, String paymentMethod)
            throws SQLException, ValidationException {
        int bookingId = booking.getBookingId();
        List<Ticket> tickets = bookingDAO.findTickets(conn, bookingId);
        Set<Integer> held = bookingDAO.findLiveHeldSeatIds(conn, bookingHoldSession(bookingId), true);
        if (!held.equals(ticketSeatIds(tickets))) {
            throw error(ERROR_ENDED, Messages.BOOKING_HOLD_EXPIRED);
        }
        Map<Integer, String> names = bookingDAO.findConcessionNames(conn, bookingId);
        for (BookingConcession item : bookingDAO.findBookingConcessions(conn, bookingId)) {
            if (!bookingDAO.decreaseStock(conn, item.getConcessionId(), item.getQuantity())) {
                throw error(ERROR_GENERAL, Messages.format(Messages.BOOKING_CONCESSION_UNAVAILABLE,
                        names.getOrDefault(item.getConcessionId(), "đã chọn")));
            }
        }
        if (booking.getVoucherId() != null) {
            Voucher voucher = bookingDAO.findVoucherByCode(conn,
                    bookingDAO.findVoucherCode(conn, booking.getVoucherId()));
            if (voucher != null && voucher.getOwnerUserId() != null
                    && !bookingDAO.markPersonalVoucherUsed(conn, voucher.getVoucherId())) {
                throw error(ERROR_GENERAL, Messages.BOOKING_VOUCHER_INVALID);
            }
        }
        if (booking.getPointsRedeemed() > 0
                && !bookingDAO.deductPoints(conn, customerId, booking.getPointsRedeemed())) {
            throw error(ERROR_GENERAL, Messages.BOOKING_POINTS_INSUFFICIENT);
        }
        for (Ticket ticket : tickets) {
            ticket.setQrCode(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
        }
        bookingDAO.updateTicketQrCodes(conn, tickets);
        if (!bookingDAO.markBookingPaid(conn, bookingId, paymentMethod)) {
            throw new SQLException("Booking status changed before payment settlement.");
        }
        bookingDAO.deleteHoldsBySession(conn, bookingHoldSession(bookingId));
    }

    private Set<Integer> ticketSeatIds(List<Ticket> tickets) {
        Set<Integer> seatIds = new HashSet<>();
        for (Ticket ticket : tickets) {
            seatIds.add(ticket.getSeatId());
        }
        return seatIds;
    }

    private int cancelExpiredBookings(Connection conn, int showtimeId) throws SQLException {
        List<Integer> expired = bookingDAO.findExpiredPendingBookingIds(conn, showtimeId);
        for (Integer bookingId : expired) {
            bookingDAO.cancelPendingBooking(conn, bookingId);
            bookingDAO.deleteHoldsBySession(conn, bookingHoldSession(bookingId));
        }
        return expired.size();
    }

    // Đơn Pending đã hết giờ giữ ghế thì hủy ngay, không đợi JOB-01 (E1)
    private void expireIfNeeded(Connection conn, User customer, int bookingId) throws SQLException {
        Booking booking = bookingDAO.findBookingOfCustomer(conn, bookingId, customer.getUserId(), true);
        if (booking != null && BookingStatus.PENDING.equals(booking.getStatus())
                && bookingDAO.findLiveHeldSeatIds(conn, bookingHoldSession(bookingId), false).isEmpty()) {
            bookingDAO.cancelPendingBooking(conn, bookingId);
            bookingDAO.deleteHoldsBySession(conn, bookingHoldSession(bookingId));
        }
    }

    private Booking lockPendingBooking(Connection conn, User customer, int bookingId)
            throws SQLException, ValidationException {
        Booking booking = bookingDAO.findBookingOfCustomer(conn, bookingId, customer.getUserId(), true);
        if (booking == null) {
            throw error(ERROR_GENERAL, Messages.BOOKING_NOT_FOUND);
        }
        if (!BookingStatus.PENDING.equals(booking.getStatus())) {
            throw error(ERROR_GENERAL, Messages.BOOKING_NOT_PENDING);
        }
        return booking;
    }

    // Cùng điều kiện mở bán với SeatMapService.load (UC-09): suất Active, phim công khai (GB-11),
    // chưa bắt đầu, phòng không bị khóa bảo trì (E2 của UC-09)
    private ShowtimeInfo loadBookableShowtime(Connection conn, int showtimeId)
            throws SQLException, ValidationException {
        Showtime showtime = showtimeId > 0 ? bookingDAO.findShowtimeWithRoom(conn, showtimeId) : null;
        if (showtime == null || !ShowtimeStatus.ACTIVE.equals(showtime.getStatus())) {
            throw error(ERROR_GENERAL, Messages.SEAT_MAP_SHOWTIME_NOT_FOUND);
        }
        Movie movie = bookingDAO.findMovie(conn, showtime.getMovieId());
        if (movie == null || !MovieStatus.PUBLIC.contains(movie.getStatus())) {
            throw error(ERROR_GENERAL, Messages.SEAT_MAP_SHOWTIME_NOT_FOUND);
        }
        if (!showtime.getStartTime().isAfter(LocalDateTime.now())) {
            throw error(ERROR_GENERAL, Messages.SEAT_MAP_SHOWTIME_STARTED);
        }
        if (RoomStatus.MAINTENANCE.equals(showtime.getRoom().getStatus())) {
            throw error(ERROR_GENERAL, Messages.SEAT_MAP_ROOM_MAINTENANCE);
        }
        return new ShowtimeInfo(showtime, movie, showtime.getRoom());
    }

    // Suất chiếu còn mở bán, ghế của phiên còn được giữ, ghế không bị khóa bảo trì giữa chừng.
    // lockHolds = true khi gọi trong transaction chốt đơn để khóa các dòng Seat_Hold.
    private CheckoutContext loadContext(Connection conn, BookingCheckout checkout, boolean lockHolds)
            throws SQLException, ValidationException {
        if (checkout == null) {
            throw error(ERROR_ENDED, Messages.BOOKING_HOLD_EXPIRED);
        }
        ShowtimeInfo info;
        try {
            info = loadBookableShowtime(conn, checkout.getShowtimeId());
        } catch (ValidationException e) {
            throw error(ERROR_ENDED, Messages.BOOKING_SHOWTIME_UNAVAILABLE);
        }
        Set<Integer> held = bookingDAO.findLiveHeldSeatIds(conn, checkout.getHoldSessionId(), lockHolds);
        if (!held.equals(new HashSet<>(checkout.getSeatIds()))) {
            throw error(ERROR_ENDED, Messages.BOOKING_HOLD_EXPIRED);
        }
        long remaining = bookingDAO.findRemainingHoldSeconds(conn, checkout.getHoldSessionId());
        Map<Integer, Seat> roomSeats = bookingDAO.findRoomSeats(conn, info.room.getRoomId());
        Map<String, BigDecimal> prices = seatPrices(conn, info.showtime.getBasePrice());
        List<BookingSeatLine> seats = new ArrayList<>();
        BigDecimal ticketTotal = BigDecimal.ZERO;
        for (Integer seatId : checkout.getSeatIds()) {
            Seat seat = roomSeats.get(seatId);
            if (seat == null || RoomStatus.MAINTENANCE.equals(seat.getStatus())) {
                throw error(ERROR_ENDED, Messages.BOOKING_SEAT_TAKEN);
            }
            BigDecimal price = prices.get(seat.getSeatType());
            seats.add(new BookingSeatLine(seatId, seat.getRowName() + seat.getColNumber(), seat.getSeatType(), price));
            ticketTotal = ticketTotal.add(price);
        }
        return new CheckoutContext(info.showtime, info.movie, info.room, seats, ticketTotal, remaining);
    }

    private CheckoutView checkoutView(Connection conn, User customer, CheckoutContext context,
                                      Map<Integer, Concession> forSale, Map<Integer, Integer> picked,
                                      BookingQuote quote, Map<String, String> errors) throws SQLException {
        return new CheckoutView(context.showtime, context.movie, context.room, context.seats,
                concessionLines(forSale, picked), quote, walletVouchers(conn, customer, quote),
                availablePoints(conn, customer.getUserId()), pointRate(conn), context.remainingSeconds, errors);
    }

    // Bước 7: voucher còn hiệu lực trong ví của khách để chọn nhanh thay vì phải nhớ mã.
    // Cùng điều kiện với isVoucherUsable: đủ đơn tối thiểu (GB-05), voucher cá nhân chưa nằm trong đơn khác.
    private List<CheckoutVoucherOption> walletVouchers(Connection conn, User customer, BookingQuote quote)
            throws SQLException {
        BigDecimal gross = quote.getGrossTotal();
        List<CheckoutVoucherOption> options = new ArrayList<>();
        for (Voucher voucher : bookingDAO.findActiveVouchersOfOwner(conn, customer.getUserId())) {
            boolean applied = voucher.getVoucherCode().equals(quote.getVoucherCode());
            boolean belowMinOrder = gross.compareTo(voucher.getMinOrderAmount()) < 0;
            boolean inOtherOrder = !applied && bookingDAO.isVoucherInUse(conn, voucher.getVoucherId(), null, 0);
            options.add(new CheckoutVoucherOption(voucher.getVoucherCode(), voucher.getDiscountType(),
                    voucher.getDiscountValue(), voucher.getMinOrderAmount(), voucher.getExpiryDate(),
                    VoucherType.REFUND_80.equals(voucher.getVoucherType()),
                    belowMinOrder ? BigDecimal.ZERO : discountOf(voucher, gross), belowMinOrder, inOtherOrder,
                    applied));
        }
        return options;
    }

    private Applied apply(Connection conn, User customer, BookingCheckout checkout, CheckoutContext context,
                          CheckoutForm form) throws SQLException {
        Map<Integer, Concession> forSale = concessionsForSale(conn);
        Map<String, String> errors = new LinkedHashMap<>();
        Map<Integer, Integer> picked = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : form.getQuantities().entrySet()) {
            String raw = entry.getValue() == null ? "" : entry.getValue().trim();
            if (raw.isEmpty() || "0".equals(raw)) {
                continue;
            }
            Concession concession = forSale.get(entry.getKey());
            if (concession == null) {
                errors.put(ERROR_CONCESSION, Messages.format(Messages.BOOKING_CONCESSION_UNAVAILABLE, "đã chọn"));
                continue;
            }
            int max = maxQuantity(concession);
            int quantity = parseNonNegative(raw);
            if (quantity < 0 || quantity > max) {
                errors.put(ERROR_CONCESSION, Messages.format(Messages.BOOKING_CONCESSION_QUANTITY_INVALID,
                        concession.getName(), max));
                continue;
            }
            if (quantity > 0) {
                picked.put(concession.getConcessionId(), quantity);
            }
        }

        String rawPoints = form.getPoints() == null ? "" : form.getPoints().trim();
        int points = rawPoints.isEmpty() ? 0 : parseNonNegative(rawPoints);
        if (points < 0) {
            errors.put(ERROR_POINTS, Messages.BOOKING_POINTS_INVALID);
            points = 0;
        }
        String voucherCode = form.getVoucherCode() == null ? ""
                : form.getVoucherCode().trim().toUpperCase(Locale.ROOT);
        BookingQuote quote = quote(conn, customer, context.ticketTotal, picked, forSale, voucherCode, points, errors);
        remember(checkout, picked, quote);
        return new Applied(quote, forSale, errors);
    }

    // Bước 8. Voucher (GB-05) không hợp lệ -> bỏ voucher (E3); điểm vượt số dư -> bỏ điểm (E4)
    private BookingQuote quote(Connection conn, User customer, BigDecimal ticketTotal, Map<Integer, Integer> picked,
                               Map<Integer, Concession> forSale, String voucherCode, int points,
                               Map<String, String> errors) throws SQLException {
        BigDecimal concessionTotal = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : picked.entrySet()) {
            concessionTotal = concessionTotal.add(forSale.get(entry.getKey()).getPrice()
                    .multiply(BigDecimal.valueOf(entry.getValue())));
        }
        BigDecimal gross = ticketTotal.add(concessionTotal);

        Voucher voucher = null;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isEmpty()) {
            voucher = bookingDAO.findVoucherByCode(conn, voucherCode);
            if (isVoucherUsable(conn, customer, voucher, gross)) {
                voucherDiscount = discountOf(voucher, gross);
            } else {
                errors.put(ERROR_VOUCHER, Messages.BOOKING_VOUCHER_INVALID);
                voucher = null;
            }
        }

        int rate = pointRate(conn);
        // BR-LOYALTY-01: điểm chỉ trừ vào tiền vé và không làm tổng tiền âm
        BigDecimal pointCap = ticketTotal.min(gross.subtract(voucherDiscount));
        int maxByAmount = pointCap.divide(BigDecimal.valueOf(rate), 0, RoundingMode.DOWN).intValue();
        int available = availablePoints(conn, customer.getUserId());
        if (points > available) {
            errors.put(ERROR_POINTS, Messages.BOOKING_POINTS_INSUFFICIENT);
            points = 0;
        } else if (points > maxByAmount) {
            errors.put(ERROR_POINTS, Messages.format(Messages.BOOKING_POINTS_EXCEED, maxByAmount));
            points = 0;
        }
        return new BookingQuote(ticketTotal, concessionTotal,
                voucher == null ? null : voucher.getVoucherId(), voucher == null ? null : voucher.getVoucherCode(),
                voucherDiscount, points, BigDecimal.valueOf((long) points * rate),
                Math.max(0, Math.min(available, maxByAmount)));
    }

    // GB-05: voucher còn hiệu lực, công khai hoặc thuộc chính khách, đơn đạt giá trị tối thiểu.
    // Voucher công khai: mỗi khách dùng một lần. Voucher cá nhân: dùng một lần (đơn Pending còn hạn cũng tính).
    private boolean isVoucherUsable(Connection conn, User customer, Voucher voucher, BigDecimal gross)
            throws SQLException {
        if (voucher == null || !VoucherService.ACTIVE.equals(voucher.getStatus())) {
            return false;
        }
        Integer owner = voucher.getOwnerUserId();
        if (owner != null && owner != customer.getUserId()) {
            return false;
        }
        if (gross.compareTo(voucher.getMinOrderAmount()) < 0) {
            return false;
        }
        return !bookingDAO.isVoucherInUse(conn, voucher.getVoucherId(),
                owner == null ? Integer.valueOf(customer.getUserId()) : null, 0);
    }

    private BigDecimal discountOf(Voucher voucher, BigDecimal gross) {
        BigDecimal discount = VoucherService.PERCENT.equals(voucher.getDiscountType())
                ? gross.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        return discount.min(gross).setScale(0, RoundingMode.HALF_UP);
    }

    // Điểm có thể dùng = số dư - điểm đã đặt vào các đơn Pending khác còn hiệu lực
    private int availablePoints(Connection conn, int customerId) throws SQLException {
        Integer balance = bookingDAO.findPointBalance(conn, customerId, false);
        int reserved = bookingDAO.sumReservedPoints(conn, customerId, 0);
        return Math.max(0, (balance == null ? 0 : balance) - reserved);
    }

    private void remember(BookingCheckout checkout, Map<Integer, Integer> picked, BookingQuote quote) {
        checkout.setConcessions(picked);
        checkout.setVoucherCode(quote.getVoucherCode());
        checkout.setPointsRedeemed(quote.getPointsRedeemed());
    }

    private Map<Integer, Concession> concessionsForSale(Connection conn) throws SQLException {
        Map<Integer, Concession> forSale = new LinkedHashMap<>();
        for (Concession concession : bookingDAO.findConcessionsForSale(conn)) {
            forSale.put(concession.getConcessionId(), concession);
        }
        return forSale;
    }

    private List<BookingConcessionLine> concessionLines(Map<Integer, Concession> forSale,
                                                        Map<Integer, Integer> picked) {
        List<BookingConcessionLine> lines = new ArrayList<>();
        for (Concession concession : forSale.values()) {
            lines.add(new BookingConcessionLine(concession.getConcessionId(), concession.getName(),
                    concession.getDescription(), concession.getImageUrl(), concession.getPrice(),
                    maxQuantity(concession), picked.getOrDefault(concession.getConcessionId(), 0)));
        }
        return lines;
    }

    private int maxQuantity(Concession concession) {
        return Math.min(concession.getStockQuantity(), MAX_CONCESSION_QUANTITY);
    }

    // Giá từng loại ghế theo TicketPricing (dùng chung với UC-19)
    private Map<String, BigDecimal> seatPrices(Connection conn, BigDecimal basePrice) throws SQLException {
        BigDecimal vipSurcharge = configDAO.getDecimal(conn, CONFIG_VIP_SURCHARGE, DEFAULT_VIP_SURCHARGE);
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        for (String type : SeatType.ALL) {
            prices.put(type, TicketPricing.priceOf(type, basePrice, vipSurcharge));
        }
        return prices;
    }

    private boolean isAgeRestricted(Movie movie) {
        return movie != null && AGE_RESTRICTED.contains(movie.getAgeRating());
    }

    private void requireCustomer(User customer) throws ValidationException {
        if (customer == null || !Role.CUSTOMER.equals(customer.getRole())) {
            throw error(ERROR_GENERAL, Messages.BOOKING_CUSTOMER_ONLY);
        }
    }

    // GB-01: thời gian giữ ghế đọc từ cấu hình, không hard-code
    private int holdMinutes(Connection conn) throws SQLException {
        return Math.max(1, configDAO.getInt(conn, CONFIG_HOLD_MINUTES, DEFAULT_HOLD_MINUTES));
    }

    private int pointRate(Connection conn) throws SQLException {
        return Math.max(1, configDAO.getInt(conn, CONFIG_POINT_RATE, DEFAULT_POINT_RATE));
    }

    private int maxSeats(Connection conn) throws SQLException {
        return Math.max(1, configDAO.getInt(conn, CONFIG_MAX_SEATS, DEFAULT_MAX_SEATS));
    }

    private String bookingHoldSession(int bookingId) {
        return BookingDAO.BOOKING_HOLD_PREFIX + bookingId;
    }

    private String newTicketCode() {
        StringBuilder code = new StringBuilder("TK");
        for (int i = 0; i < TICKET_CODE_LENGTH; i++) {
            code.append(TICKET_CODE_CHARS.charAt(RANDOM.nextInt(TICKET_CODE_CHARS.length())));
        }
        return code.toString();
    }

    private int parseNonNegative(String value) {
        try {
            return Math.max(-1, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isDuplicateKey(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                for (SQLException next = sql; next != null; next = next.getNextException()) {
                    if (DUPLICATE_KEY_ERRORS.contains(next.getErrorCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ValidationException error(String key, String message) {
        return new ValidationException(Map.of(key, message));
    }

    private <T> T inTransaction(TransactionWork<T> work) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T run(Connection conn) throws SQLException, ValidationException;
    }

    private static final class ShowtimeInfo {
        private final Showtime showtime;
        private final Movie movie;
        private final Room room;

        private ShowtimeInfo(Showtime showtime, Movie movie, Room room) {
            this.showtime = showtime;
            this.movie = movie;
            this.room = room;
        }
    }

    private static final class CheckoutContext {
        private final Showtime showtime;
        private final Movie movie;
        private final Room room;
        private final List<BookingSeatLine> seats;
        private final BigDecimal ticketTotal;
        private final long remainingSeconds;

        private CheckoutContext(Showtime showtime, Movie movie, Room room, List<BookingSeatLine> seats,
                                BigDecimal ticketTotal, long remainingSeconds) {
            this.showtime = showtime;
            this.movie = movie;
            this.room = room;
            this.seats = seats;
            this.ticketTotal = ticketTotal;
            this.remainingSeconds = remainingSeconds;
        }
    }

    private static final class Applied {
        private final BookingQuote quote;
        private final Map<Integer, Concession> forSale;
        private final Map<String, String> errors;

        private Applied(BookingQuote quote, Map<Integer, Concession> forSale, Map<String, String> errors) {
            this.quote = quote;
            this.forSale = forSale;
            this.errors = errors;
        }
    }
}
