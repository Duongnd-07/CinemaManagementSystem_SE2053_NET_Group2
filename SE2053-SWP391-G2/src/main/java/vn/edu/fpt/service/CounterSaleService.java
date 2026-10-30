package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vn.edu.fpt.dao.BookingDAO;
import vn.edu.fpt.dao.ConcessionDAO;
import vn.edu.fpt.dao.CounterSaleDAO;
import vn.edu.fpt.dao.ShiftDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.model.Booking;
import vn.edu.fpt.model.BookingChannel;
import vn.edu.fpt.model.BookingConcession;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.PaymentMethod;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.Ticket;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.model.User;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-19: Sell Ticket & Concession at Counter.
// Nhân viên phải mở ca trước (đơn gắn ShiftID cho báo cáo ca UC-22). Tiền do nhân viên thu tay (tiền mặt, chuyển
// khoản/QR, thẻ POS) nên không qua cổng thanh toán: xác nhận là tạo đơn Paid, vé, trừ kho, điểm, voucher trong một
// transaction. Quy tắc giá, voucher (GB-05), điểm (BR-LOYALTY-01) giống đặt vé online UC-13 để hai kênh tính như nhau.
public class CounterSaleService {
    public static final String ERROR_GENERAL = "general";
    public static final String ERROR_SHIFT = "shift";
    public static final String ERROR_SEATS = "seats";
    public static final String ERROR_CONCESSION = "concession";
    public static final String ERROR_CUSTOMER = "customer";
    public static final String ERROR_VOUCHER = "voucher";
    public static final String ERROR_POINTS = "points";
    public static final String ERROR_PAYMENT = "payment";
    public static final String ERROR_AGE = "age";

    public static final List<String> PAYMENT_METHODS =
            List.of(PaymentMethod.CASH, PaymentMethod.TRANSFER, PaymentMethod.CARD);

    private static final String SHIFT_REQUIRED = "Bạn cần mở ca trước khi bán vé.";
    private static final String SHIFT_EXPIRED = "Ca của bạn đã hết giờ, đơn chưa được bán. Vui lòng mở ca mới.";
    private static final String SEAT_REQUIRED = "Vui lòng chọn ít nhất một ghế.";
    private static final String SEAT_UNAVAILABLE = "Ghế {0} vừa được bán hoặc đang được giữ, vui lòng chọn ghế khác.";
    private static final String CUSTOMER_NOT_FOUND = "Không tìm thấy khách thành viên với số điện thoại/email này.";
    private static final String MEMBER_ONLY = "Chỉ khách thành viên mới được dùng voucher hoặc điểm.";
    private static final String AGE_REQUIRED = "Phim giới hạn độ tuổi {0}: hãy kiểm tra giấy tờ của khách rồi tích xác nhận.";
    private static final String PAYMENT_INVALID = "Vui lòng chọn hình thức thanh toán.";
    private static final String CASH_INVALID = "Tiền khách đưa không hợp lệ.";
    private static final String CASH_SHORT = "Tiền khách đưa chưa đủ, còn thiếu {0}đ.";
    private static final String CARD_REFERENCE_INVALID = "Nhập mã chuẩn chi 6 số in trên hóa đơn máy POS sau khi cà thẻ thành công.";
    private static final String TRANSFER_REFERENCE_INVALID =
            "Nhập mã giao dịch ngân hàng (6-30 chữ/số) trong thông báo đã nhận tiền.";
    private static final String TRANSFER_REFERENCE_USED = "Mã giao dịch này đã được dùng cho đơn #{0}, kiểm tra lại giao dịch.";
    private static final String TRANSFER_REFERENCE_TAKEN = "Mã giao dịch này vừa được dùng cho đơn khác, kiểm tra lại giao dịch.";
    private static final String TRANSFER_REFERENCE_INDEX = "UX_Booking_TransferReference";

    // Cùng cấu hình và giá trị mặc định với BookingService (UC-13)
    private static final List<String> AGE_RESTRICTED = List.of("C13", "C16", "C18");
    private static final String CONFIG_POINT_RATE = "POINT_EXCHANGE_RATE";
    private static final String CONFIG_POINT_EARN_PERCENT = "POINT_EARN_PERCENT";
    private static final String CONFIG_VIP_SURCHARGE = "SEAT_SURCHARGE_VIP";
    private static final int DEFAULT_POINT_RATE = 1000;
    private static final BigDecimal DEFAULT_VIP_SURCHARGE = BigDecimal.valueOf(30000);
    private static final Set<Integer> DUPLICATE_KEY_ERRORS = Set.of(2601, 2627);
    private static final String TICKET_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TICKET_CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    // Trạng thái tài khoản khách còn hoạt động (giống AuthFilter)
    private static final String ACTIVE = "Active";

    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final ShiftService shiftService = new ShiftService();
    private final CounterSaleDAO counterDAO = new CounterSaleDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final ConcessionDAO concessionDAO = new ConcessionDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();
    private final UserDAO userDAO = new UserDAO();
    private final BookingService bookingService = new BookingService();

    // ===== Bước 1-3: chọn suất, ghế, bắp nước =====

    public List<Showtime> listShowtimes(LocalDate date) throws SQLException {
        return counterDAO.findSellableShowtimes(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // Sơ đồ ghế và giá từng loại ghế dùng chung với UC-09/UC-13
    public BookingSeatView loadSeats(int showtimeId) throws SQLException, ValidationException {
        return bookingService.loadSeatSelection(showtimeId);
    }

    // Món đang bán và còn hàng khi mới mở màn hình bán (chưa chọn số lượng)
    public List<BookingConcessionLine> listConcessions() throws SQLException {
        List<BookingConcessionLine> lines = new ArrayList<>();
        for (Concession concession : concessionDAO.findAvailableForSale()) {
            lines.add(new BookingConcessionLine(concession.getConcessionId(), concession.getName(),
                    concession.getDescription(), concession.getImageUrl(), concession.getPrice(),
                    concession.getStockQuantity(), 0));
        }
        return lines;
    }

    // ===== Bước 4-5: tra thành viên, tính tiền =====

    public CounterQuote quote(CounterSaleForm form) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            CounterQuote quote = price(conn, form, false);
            checkPayment(conn, form, quote, false);
            return quote;
        }
    }

    // ===== Bước 6-8: xác nhận đã thu tiền =====

    // Trả về tiền đã tính kèm BookingID vừa tạo; mọi lỗi (kể cả ghế vừa bị bán ở quầy khác) làm hủy toàn bộ giao dịch (E1)
    public CounterQuote confirm(CounterSaleForm form, int staffId) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Shift shift = shiftDAO.findOpenByStaff(conn, staffId, true);
                if (shift == null) {
                    throw error(ERROR_SHIFT, SHIFT_REQUIRED);
                }
                // Ca quá giờ mà tác vụ nền (chạy mỗi phút) chưa kịp kết: không cho đơn lọt vào sau giờ kết ca
                if (shiftService.isExpired(shift, shiftService.shiftHours())) {
                    throw error(ERROR_SHIFT, SHIFT_EXPIRED);
                }
                CounterQuote quote = price(conn, form, true);
                checkPayment(conn, form, quote, true);
                if (!quote.isValid()) {
                    throw new ValidationException(quote.errors);
                }
                quote.bookingId = save(conn, form, quote, staffId, shift.getShiftId());
                conn.commit();
                return quote;
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                // Hai quầy nhập cùng một mã chuyển khoản cùng lúc: UX_Booking_TransferReference chặn đơn đến sau
                if (e instanceof SQLException sql && isDuplicateKey(sql)
                        && String.valueOf(sql.getMessage()).contains(TRANSFER_REFERENCE_INDEX)) {
                    throw error(ERROR_PAYMENT, TRANSFER_REFERENCE_TAKEN);
                }
                // Hai quầy bán cùng ghế cùng lúc: UX_Ticket_Showtime_Seat chặn giao dịch đến sau
                if (e instanceof SQLException sql && isDuplicateKey(sql)) {
                    throw error(ERROR_SEATS, Messages.BOOKING_SEAT_TAKEN);
                }
                throw e;
            }
        }
    }

    public CounterReceipt findReceipt(int bookingId) throws SQLException {
        return counterDAO.findReceipt(bookingId);
    }

    // ===== Nội bộ =====

    // Tính lại toàn bộ từ dữ liệu DB, không tin giá gửi từ trình duyệt. lock = true khi đang trong transaction chốt đơn
    private CounterQuote price(Connection conn, CounterSaleForm form, boolean lock)
            throws SQLException, ValidationException {
        CounterQuote quote = new CounterQuote();
        Showtime showtime = loadSellableShowtime(conn, form.getShowtimeId());
        Movie movie = showtime.getMovie();
        if (AGE_RESTRICTED.contains(movie.getAgeRating()) && !form.isAgeConfirmed()) {
            quote.errors.put(ERROR_AGE, Messages.format(AGE_REQUIRED, movie.getAgeRating()));
        }
        priceSeats(conn, form, showtime, quote);
        priceConcessions(conn, form, quote);

        String identifier = form.getCustomer().trim();
        if (!identifier.isEmpty()) {
            User customer = userDAO.findByEmailOrPhone(identifier);
            if (customer == null || !Role.CUSTOMER.equals(customer.getRole()) || !ACTIVE.equals(customer.getStatus())) {
                quote.errors.put(ERROR_CUSTOMER, CUSTOMER_NOT_FOUND);
            } else {
                quote.customer = customer;
            }
        }
        priceVoucher(conn, form, quote, showtime.getMovieId());
        pricePoints(conn, form, quote, lock);
        return quote;
    }

    // Cùng điều kiện mở bán với UC-09/UC-13: suất Active chưa bắt đầu, phim công khai, phòng đang hoạt động
    private Showtime loadSellableShowtime(Connection conn, int showtimeId) throws SQLException, ValidationException {
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
        if (!RoomStatus.ACTIVE.equals(showtime.getRoom().getStatus())) {
            throw error(ERROR_GENERAL, Messages.SEAT_MAP_ROOM_MAINTENANCE);
        }
        showtime.setMovie(movie);
        return showtime;
    }

    private void priceSeats(Connection conn, CounterSaleForm form, Showtime showtime, CounterQuote quote)
            throws SQLException {
        List<Integer> seatIds = new ArrayList<>(toIds(form.getSeatIds()));
        if (seatIds.isEmpty()) {
            quote.errors.put(ERROR_SEATS, SEAT_REQUIRED);
            return;
        }
        Map<Integer, Seat> roomSeats = bookingDAO.findRoomSeats(conn, showtime.getRoomId());
        if (!roomSeats.keySet().containsAll(seatIds)) {
            quote.errors.put(ERROR_SEATS, Messages.BOOKING_SEAT_INVALID);
            return;
        }
        // Đơn online quá hạn và ghế giữ quá hạn không còn chiếm ghế (giống UC-13 trước khi giữ ghế)
        for (Integer bookingId : bookingDAO.findExpiredPendingBookingIds(conn, showtime.getShowtimeId())) {
            bookingDAO.cancelPendingBooking(conn, bookingId);
            bookingDAO.deleteHoldsBySession(conn, BookingDAO.BOOKING_HOLD_PREFIX + bookingId);
        }
        bookingDAO.deleteExpiredHolds(conn, showtime.getShowtimeId());
        // Bán tại quầy không giữ ghế 10 phút, nhưng không lấy ghế khách online đang giữ hoặc ghế bảo trì
        Set<Integer> unavailable = bookingDAO.findUnavailableSeatIds(conn, showtime.getShowtimeId(), seatIds);

        BigDecimal vipSurcharge = configDAO.getDecimal(conn, CONFIG_VIP_SURCHARGE, DEFAULT_VIP_SURCHARGE);
        List<String> taken = new ArrayList<>();
        for (Integer seatId : seatIds) {
            Seat seat = roomSeats.get(seatId);
            String label = seat.getRowName() + seat.getColNumber();
            if (unavailable.contains(seatId)) {
                taken.add(label);
                continue;
            }
            BigDecimal price = TicketPricing.priceOf(seat.getSeatType(), showtime.getBasePrice(), vipSurcharge);
            quote.seats.add(new BookingSeatLine(seatId, label, seat.getSeatType(), price));
            quote.ticketTotal = quote.ticketTotal.add(price);
        }
        if (!taken.isEmpty()) {
            quote.errors.put(ERROR_SEATS, Messages.format(SEAT_UNAVAILABLE, String.join(", ", taken)));
        }
    }

    // Tại quầy không giới hạn 10 món như online, chỉ không vượt tồn kho
    private void priceConcessions(Connection conn, CounterSaleForm form, CounterQuote quote) throws SQLException {
        List<String> problems = new ArrayList<>();
        Map<Integer, Concession> forSale = new LinkedHashMap<>();
        for (Concession concession : bookingDAO.findConcessionsForSale(conn)) {
            forSale.put(concession.getConcessionId(), concession);
        }
        for (Map.Entry<Integer, String> entry : form.getQuantities().entrySet()) {
            String raw = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!raw.isEmpty() && !"0".equals(raw) && !forSale.containsKey(entry.getKey())) {
                problems.add(Messages.format(Messages.BOOKING_CONCESSION_UNAVAILABLE, "đã chọn"));
            }
        }
        for (Concession concession : forSale.values()) {
            String raw = form.getQuantities().getOrDefault(concession.getConcessionId(), "").trim();
            int quantity = raw.isEmpty() ? 0 : parseNonNegative(raw);
            if (quantity < 0 || quantity > concession.getStockQuantity()) {
                problems.add(Messages.format(Messages.BOOKING_CONCESSION_QUANTITY_INVALID, concession.getName(),
                        concession.getStockQuantity()));
                quantity = 0;
            }
            BookingConcessionLine line = new BookingConcessionLine(concession.getConcessionId(), concession.getName(),
                    concession.getDescription(), concession.getImageUrl(), concession.getPrice(),
                    concession.getStockQuantity(), quantity);
            quote.concessions.add(line);
            quote.concessionTotal = quote.concessionTotal.add(line.getSubtotal());
        }
        if (!problems.isEmpty()) {
            quote.errors.put(ERROR_CONCESSION, String.join(" ", problems));
        }
    }

    // GB-05 giống UC-13: còn hiệu lực, công khai hoặc thuộc chính khách, đạt đơn tối thiểu, chưa hết lượt
    private void priceVoucher(Connection conn, CounterSaleForm form, CounterQuote quote, int movieId)
            throws SQLException {
        // Phim chỉ giới hạn điều kiện áp dụng; giá trị giảm vẫn tính trên toàn đơn gồm vé và bắp nước.
        BigDecimal gross = quote.getGrossTotal();
        if (quote.customer != null) {
            for (Voucher voucher : bookingDAO.findActiveVouchersOfOwner(conn, quote.customer.getUserId(), movieId)) {
                quote.vouchers.add(new CheckoutVoucherOption(voucher.getVoucherCode(), voucher.getDiscountType(),
                        voucher.getDiscountValue(), voucher.getMinOrderAmount(), voucher.getExpiryDate(),
                        gross.compareTo(voucher.getMinOrderAmount()) < 0,
                        bookingDAO.isVoucherInUse(conn, voucher.getVoucherId(), null, 0),
                        voucher.getVoucherCode().equalsIgnoreCase(form.getVoucherCode().trim())));
            }
        }
        String code = form.getVoucherCode().trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return;
        }
        if (quote.customer == null) {
            quote.errors.put(ERROR_VOUCHER, MEMBER_ONLY);
            return;
        }
        Voucher voucher = bookingDAO.findVoucherByCode(conn, code);
        if (!isVoucherUsable(conn, quote.customer, voucher, gross, movieId)) {
            quote.errors.put(ERROR_VOUCHER, Messages.BOOKING_VOUCHER_INVALID);
            return;
        }
        BigDecimal discount = VoucherService.PERCENT.equals(voucher.getDiscountType())
                ? gross.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        quote.voucherDiscount = discount.min(gross).setScale(0, RoundingMode.HALF_UP);
        quote.voucherId = voucher.getVoucherId();
        quote.voucherCode = voucher.getVoucherCode();
    }

    private boolean isVoucherUsable(Connection conn, User customer, Voucher voucher, BigDecimal gross, int movieId)
            throws SQLException {
        if (voucher == null || !VoucherService.ACTIVE.equals(voucher.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() == null || voucher.getExpiryDate() == null
                || voucher.getStartDate().isAfter(now) || !voucher.getExpiryDate().isAfter(now)) {
            return false;
        }
        Integer owner = voucher.getOwnerUserId();
        if (owner != null && owner != customer.getUserId()) {
            return false;
        }
        if (voucher.getMovieId() != null && voucher.getMovieId() != movieId) {
            return false;
        }
        if (gross.compareTo(voucher.getMinOrderAmount()) < 0) {
            return false;
        }
        if (voucher.getUsageLimit() != null
                && bookingDAO.countActiveVoucherUses(conn, voucher.getVoucherId()) >= voucher.getUsageLimit()) {
            return false;
        }
        if (owner == null) {
            int perUserLimit = voucher.getPerUserUsageLimit() == null ? 1 : voucher.getPerUserUsageLimit();
            return bookingDAO.countActiveVoucherUsesByCustomer(conn, voucher.getVoucherId(),
                    customer.getUserId()) < perUserLimit;
        }
        return !bookingDAO.isVoucherInUse(conn, voucher.getVoucherId(), null, 0);
    }

    // BR-LOYALTY-01: điểm chỉ trừ vào tiền vé, không làm tổng âm, không vượt số điểm còn dùng được
    private void pricePoints(Connection conn, CounterSaleForm form, CounterQuote quote, boolean lock)
            throws SQLException {
        quote.pointRate = Math.max(1, configDAO.getInt(conn, CONFIG_POINT_RATE, DEFAULT_POINT_RATE));
        String raw = form.getPoints().trim();
        int points = raw.isEmpty() ? 0 : parseNonNegative(raw);
        if (quote.customer == null) {
            if (points != 0) {
                quote.errors.put(ERROR_POINTS, MEMBER_ONLY);
            }
            return;
        }
        // Khóa dòng User trong transaction chốt đơn để hai quầy không cùng tiêu một lượng điểm
        Integer balance = bookingDAO.findPointBalance(conn, quote.customer.getUserId(), lock);
        int reserved = bookingDAO.sumReservedPoints(conn, quote.customer.getUserId(), 0);
        quote.availablePoints = Math.max(0, (balance == null ? 0 : balance) - reserved);
        BigDecimal cap = quote.ticketTotal.min(quote.getGrossTotal().subtract(quote.voucherDiscount)).max(BigDecimal.ZERO);
        int maxByAmount = cap.divide(BigDecimal.valueOf(quote.pointRate), 0, RoundingMode.DOWN).intValue();
        quote.maxPoints = Math.min(quote.availablePoints, maxByAmount);
        if (points < 0) {
            quote.errors.put(ERROR_POINTS, Messages.BOOKING_POINTS_INVALID);
        } else if (points > quote.availablePoints) {
            quote.errors.put(ERROR_POINTS, Messages.BOOKING_POINTS_INSUFFICIENT);
        } else if (points > maxByAmount) {
            quote.errors.put(ERROR_POINTS, Messages.format(Messages.BOOKING_POINTS_EXCEED, maxByAmount));
        } else {
            quote.pointsRedeemed = points;
        }
    }

    // required = false khi chỉ tính tiền: chưa chọn hình thức thì chưa báo lỗi, nhưng vẫn tính tiền thối nếu có
    private void checkPayment(Connection conn, CounterSaleForm form, CounterQuote quote, boolean required)
            throws SQLException {
        String method = form.getPaymentMethod();
        if (!PAYMENT_METHODS.contains(method)) {
            if (required) {
                quote.errors.put(ERROR_PAYMENT, PAYMENT_INVALID);
            }
            return;
        }
        if (!PaymentMethod.CASH.equals(method)) {
            checkReference(conn, method, paymentReference(form), quote, required);
            return;
        }
        String raw = form.getCashReceived().trim().replace(".", "").replace(",", "");
        if (raw.isEmpty() && quote.getFinalAmount().signum() == 0) {
            return;
        }
        if (!raw.matches("\\d{1,12}")) {
            if (required || !raw.isEmpty()) {
                quote.errors.put(ERROR_PAYMENT, CASH_INVALID);
            }
            return;
        }
        BigDecimal cash = new BigDecimal(raw);
        if (cash.compareTo(quote.getFinalAmount()) < 0) {
            quote.errors.put(ERROR_PAYMENT, Messages.format(CASH_SHORT,
                    String.format(Locale.US, "%,d", quote.getFinalAmount().subtract(cash).longValue()).replace(',', '.')));
            return;
        }
        quote.changeDue = cash.subtract(quote.getFinalAmount());
    }

    // Thẻ / chuyển khoản không tự xác minh được với ngân hàng: bắt buộc mã giao dịch làm bằng chứng tiền đã về,
    // lưu vào đơn để Manager đối chiếu với hóa đơn POS và sao kê khi kết ca (UC-22). Đơn 0đ không cần mã
    private void checkReference(Connection conn, String method, String reference, CounterQuote quote,
                                boolean required) throws SQLException {
        if (quote.getFinalAmount().signum() == 0 || (reference.isEmpty() && !required)) {
            return;
        }
        boolean card = PaymentMethod.CARD.equals(method);
        if (!reference.matches(card ? "\\d{6}" : "[A-Z0-9]{6,30}")) {
            quote.errors.put(ERROR_PAYMENT, card ? CARD_REFERENCE_INVALID : TRANSFER_REFERENCE_INVALID);
            return;
        }
        Integer usedBy = card ? null : counterDAO.findBookingIdByTransferReference(conn, reference);
        if (usedBy != null) {
            quote.errors.put(ERROR_PAYMENT, Messages.format(TRANSFER_REFERENCE_USED, String.valueOf(usedBy)));
        }
    }

    // Mã giao dịch viết hoa, bỏ khoảng trắng; tiền mặt không lưu mã
    private String paymentReference(CounterSaleForm form) {
        return form.getPaymentReference().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private int save(Connection conn, CounterSaleForm form, CounterQuote quote, int staffId, int shiftId)
            throws SQLException, ValidationException {
        Integer customerId = quote.customer == null ? null : quote.customer.getUserId();
        int pointsEarned = customerId == null ? 0 : earnedPoints(conn, quote);

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setStaffId(staffId);
        booking.setShiftId(shiftId);
        booking.setShowtimeId(form.getShowtimeId());
        booking.setVoucherId(quote.voucherId);
        booking.setTotalPrice(quote.getGrossTotal());
        booking.setDiscountAmount(quote.getDiscountTotal());
        booking.setFinalAmount(quote.getFinalAmount());
        booking.setPaymentMethod(form.getPaymentMethod());
        booking.setStatus(BookingStatus.PAID);
        booking.setChannel(BookingChannel.COUNTER);
        booking.setPointsRedeemed(quote.pointsRedeemed);
        booking.setPointsEarned(pointsEarned);
        boolean cash = PaymentMethod.CASH.equals(form.getPaymentMethod());
        String reference = cash ? "" : paymentReference(form);
        // checkPayment đã tính tiền thối từ tiền khách đưa; đơn 0đ không nhập tiền thì không có
        BigDecimal cashReceived = cash && quote.changeDue != null ? quote.getFinalAmount().add(quote.changeDue) : null;
        int bookingId = counterDAO.insertBooking(conn, booking, reference.isEmpty() ? null : reference, cashReceived);

        List<Ticket> tickets = new ArrayList<>();
        for (BookingSeatLine seat : quote.seats) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(bookingId);
            ticket.setSeatId(seat.getSeatId());
            ticket.setPrice(seat.getPrice());
            ticket.setTicketCode(newTicketCode());
            ticket.setStatus(TicketStatus.UPCOMING);
            ticket.setShowtimeId(form.getShowtimeId());
            tickets.add(ticket);
        }
        bookingDAO.insertTickets(conn, tickets);
        // Mã QR để soát vé, giống vé online sau khi thanh toán (UC-14)
        List<Ticket> saved = bookingDAO.findTickets(conn, bookingId);
        for (Ticket ticket : saved) {
            ticket.setQrCode(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
        }
        bookingDAO.updateTicketQrCodes(conn, saved);

        List<BookingConcession> items = new ArrayList<>();
        for (BookingConcessionLine line : quote.concessions) {
            if (line.getQuantity() == 0) {
                continue;
            }
            // GB-10: trừ kho ngay khi xác nhận; hết hàng giữa chừng thì hủy giao dịch
            if (!bookingDAO.decreaseStock(conn, line.getConcessionId(), line.getQuantity())) {
                throw error(ERROR_CONCESSION, Messages.format(Messages.BOOKING_CONCESSION_UNAVAILABLE, line.getName()));
            }
            BookingConcession item = new BookingConcession();
            item.setBookingId(bookingId);
            item.setConcessionId(line.getConcessionId());
            item.setQuantity(line.getQuantity());
            item.setSubtotalPrice(line.getSubtotal());
            items.add(item);
        }
        bookingDAO.insertBookingConcessions(conn, items);

        if (quote.voucherId != null) {
            Voucher voucher = bookingDAO.findVoucherByCode(conn, quote.voucherCode);
            if (voucher != null && voucher.getOwnerUserId() != null
                    && !bookingDAO.markPersonalVoucherUsed(conn, voucher.getVoucherId())) {
                throw error(ERROR_VOUCHER, Messages.BOOKING_VOUCHER_INVALID);
            }
        }
        if (quote.pointsRedeemed > 0) {
            if (!bookingDAO.deductPoints(conn, customerId, quote.pointsRedeemed)) {
                throw error(ERROR_POINTS, Messages.BOOKING_POINTS_INSUFFICIENT);
            }
            bookingDAO.insertLoyaltyTransaction(conn, customerId, bookingId, "Redeemed", -quote.pointsRedeemed,
                    "Điểm sử dụng cho đơn mua tại quầy");
        }
        if (pointsEarned > 0) {
            if (!bookingDAO.addPoints(conn, customerId, pointsEarned)) {
                throw new SQLException("Customer loyalty balance exceeds the supported range.");
            }
            bookingDAO.insertLoyaltyTransaction(conn, customerId, bookingId, "Earned", pointsEarned,
                    "Điểm từ đơn mua tại quầy");
        }
        return bookingId;
    }

    // Cùng công thức UC-14: (tiền vé - giảm giá) x POINT_EARN_PERCENT% / tỷ lệ quy đổi, làm tròn xuống
    private int earnedPoints(Connection conn, CounterQuote quote) throws SQLException {
        BigDecimal eligible = quote.ticketTotal.subtract(quote.getDiscountTotal()).max(BigDecimal.ZERO);
        BigDecimal percent = configDAO.getDecimal(conn, CONFIG_POINT_EARN_PERCENT, BigDecimal.ONE);
        if (percent.signum() < 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SQLException("POINT_EARN_PERCENT must be between 0 and 100.");
        }
        return eligible.multiply(percent)
                .divide(BigDecimal.valueOf(100L * quote.pointRate), 0, RoundingMode.DOWN).intValueExact();
    }

    private Set<Integer> toIds(List<String> values) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (String value : values) {
            int id = parseNonNegative(value == null ? "" : value.trim());
            if (id > 0) {
                ids.add(id);
            }
        }
        return ids;
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
        for (SQLException next = e; next != null; next = next.getNextException()) {
            if (DUPLICATE_KEY_ERRORS.contains(next.getErrorCode())) {
                return true;
            }
        }
        return false;
    }

    private ValidationException error(String key, String message) {
        return new ValidationException(Map.of(key, message));
    }
}
