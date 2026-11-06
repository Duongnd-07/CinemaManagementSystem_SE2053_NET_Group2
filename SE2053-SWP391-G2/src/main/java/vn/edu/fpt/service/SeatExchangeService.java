package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import vn.edu.fpt.dao.BookingDAO;
import vn.edu.fpt.dao.EquipmentReportDAO;
import vn.edu.fpt.dao.SeatDAO;
import vn.edu.fpt.dao.SeatExchangeDAO;
import vn.edu.fpt.dao.ShiftDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatType;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.model.Ticket;
import vn.edu.fpt.model.TicketStatus;
import vn.edu.fpt.model.UrgencyLevel;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-20: Exchange Broken Seat.
// Luôn ưu tiên ghế cùng hạng; chỉ khi hết ghế cùng hạng mới có: ghế thường -> nâng VIP miễn phí; ghế VIP -> xuống
// ghế thường, trả tiền mặt chênh lệch; ghế đôi -> 2 ghế cạnh nhau cùng loại (cùng VIP hoặc cùng thường).
// Khách không bị ép đổi ghế: luôn được chọn hoàn tiền ngay, hoàn cả bill (toàn bộ tiền khách thực trả + toàn bộ điểm
// đã dùng, kể cả bắp nước; vé còn lại của đơn vẫn giữ), mỗi đơn chỉ hoàn một lần. Mọi trường hợp (kể cả đổi cùng
// hạng) đều tặng voucher đền bù cho khách thành viên (giá trị, hạn dùng do Admin cấu hình ở UC-33).
// Ghế hỏng luôn bị khóa bảo trì và có báo cáo sự cố (UC-21) để Manager sửa xong mở lại (UC-26).
public class SeatExchangeService {
    public static final String ERROR_GENERAL = "general";
    public static final String ERROR_TARGET = "target";
    public static final String ERROR_REASON = "reason";

    public static final String TYPE_SAME = "Same";
    public static final String TYPE_UPGRADE = "Upgrade";
    public static final String TYPE_DOWNGRADE = "Downgrade";
    public static final String TYPE_SPLIT = "Split";
    public static final String TYPE_REFUND = "Refund";

    private static final int MAX_REASON = 500;
    private static final String CONFIG_VOUCHER_VALUE = "BROKEN_SEAT_VOUCHER_VALUE";
    private static final String CONFIG_VOUCHER_DAYS = "BROKEN_SEAT_VOUCHER_DAYS";
    private static final String CONFIG_VIP_SURCHARGE = "SEAT_SURCHARGE_VIP";
    private static final BigDecimal DEFAULT_VOUCHER_VALUE = BigDecimal.valueOf(50000);
    private static final int DEFAULT_VOUCHER_DAYS = 30;
    private static final BigDecimal DEFAULT_VIP_SURCHARGE = BigDecimal.valueOf(30000);
    // Cùng cấu hình và giá trị mặc định với bán vé (UC-13/UC-19)
    private static final String CONFIG_POINT_RATE = "POINT_EXCHANGE_RATE";
    private static final int DEFAULT_POINT_RATE = 1000;
    private static final Set<Integer> DUPLICATE_KEY_ERRORS = Set.of(2601, 2627);
    private static final String TICKET_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String TICKET_NOT_FOUND = "Không tìm thấy vé với mã này.";
    private static final String TICKET_NOT_VALID = "Vé không còn hiệu lực (đã đổi, đã hủy hoặc đơn chưa thanh toán).";
    private static final String SHOWTIME_ENDED = "Suất chiếu của vé đã kết thúc hoặc bị hủy.";
    private static final String TARGET_REQUIRED = "Vui lòng chọn ghế mới.";
    private static final String TARGET_TAKEN = "Ghế vừa chọn không còn trống, vui lòng chọn lại.";
    private static final String SHIFT_REQUIRED = "Cần mở ca bán vé để lấy tiền trong két trả cho khách.";
    private static final String REASON_REQUIRED = "Vui lòng ghi tình trạng ghế hỏng.";
    // Ghép chuỗi thay vì Messages.format: lý do do nhân viên gõ có thể chứa dấu ' mà MessageFormat hiểu sai
    private static final String REPORT_DESCRIPTION_PREFIX = "Khách phản ánh khi đổi ghế (vé ";
    private static final String POINTS_NOTE_PREFIX = "Hoàn điểm do ghế hỏng, vé ";

    private final SeatExchangeDAO exchangeDAO = new SeatExchangeDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final EquipmentReportDAO reportDAO = new EquipmentReportDAO();
    private final EquipmentReportService reportService = new EquipmentReportService();
    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final ShiftService shiftService = new ShiftService();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();

    public List<ExchangeTicket> listAffected() throws SQLException {
        return exchangeDAO.findAffected();
    }

    // Phương án cho vé; lỗi nếu không tìm thấy hoặc vé không còn đổi được
    public ExchangeOptions options(String ticketCode) throws SQLException, ValidationException {
        ExchangeTicket ticket = exchangeDAO.findTicketByCode(ticketCode == null ? "" : ticketCode.trim().toUpperCase(Locale.ROOT));
        requireExchangeable(ticket);
        try (Connection conn = DBContext.getConnection()) {
            return options(conn, ticket);
        }
    }

    // target: "S:12" cùng hạng, "U:12" nâng VIP, "D:12" xuống thường, "P:12-13" hai ghế cạnh nhau
    public int exchange(int ticketId, String target, String reason, int staffId)
            throws SQLException, ValidationException {
        String cleanReason = requireReason(reason);
        String value = target == null ? "" : target.trim();
        if (value.length() < 3 || value.charAt(1) != ':') {
            throw error(ERROR_TARGET, TARGET_REQUIRED);
        }
        char kind = value.charAt(0);
        String seats = value.substring(2);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExchangeTicket ticket = exchangeDAO.findTicketById(conn, ticketId, true);
                requireExchangeable(ticket);
                ExchangeOptions options = options(conn, ticket);
                List<ExchangeOptions.Choice> allowed = kind == 'S' ? options.same : kind == 'U' ? options.upgrade
                        : kind == 'D' ? options.downgrade : kind == 'P' ? options.pairs : List.of();
                if (allowed.stream().noneMatch(choice -> choice.getValue().equals(seats))) {
                    throw error(ERROR_TARGET, TARGET_TAKEN);
                }
                String type = kind == 'S' ? TYPE_SAME : kind == 'U' ? TYPE_UPGRADE
                        : kind == 'D' ? TYPE_DOWNGRADE : TYPE_SPLIT;

                // Xuống ghế thường: trả phần khách thực trả cho tiền chênh lệch VIP (cùng công thức hoàn vé: trừ phần
                // voucher, phần điểm trả lại bằng điểm, còn lại tiền mặt từ két nên phải đang trong ca)
                boolean downgrade = TYPE_DOWNGRADE.equals(type);
                Shift shift = downgrade ? requireShift(conn, staffId) : null;
                BigDecimal cash = downgrade ? options.downgradeCash : BigDecimal.ZERO;
                int points = downgrade && ticket.isMember() ? options.downgradePoints : 0;
                if (!exchangeDAO.closeTicket(conn, ticket.getTicketId(), TicketStatus.EXCHANGED,
                        cash.signum() > 0 ? cash : null, shift == null ? null : shift.getShiftId())) {
                    throw error(ERROR_GENERAL, TICKET_NOT_VALID);
                }

                List<Integer> newSeatIds = new ArrayList<>();
                for (String id : seats.split("-")) {
                    newSeatIds.add(Integer.parseInt(id));
                }
                // Vé mới mang giá ghế thường (đã trừ đủ chênh lệch), để lần hoàn sau tính đúng theo cùng công thức
                BigDecimal newTotal = downgrade
                        ? ticket.getPrice().subtract(options.vipDifference.min(ticket.getPrice())) : ticket.getPrice();
                List<BigDecimal> prices = newPrices(newTotal, newSeatIds.size());
                List<Integer> newTicketIds = createTickets(conn, ticket, newSeatIds, prices);
                if (points > 0) {
                    if (!bookingDAO.addPoints(conn, ticket.getCustomerId(), points)) {
                        throw new SQLException("Customer loyalty balance exceeds the supported range.");
                    }
                    bookingDAO.insertLoyaltyTransaction(conn, ticket.getCustomerId(), ticket.getBookingId(),
                            "Adjustment", points, POINTS_NOTE_PREFIX + ticket.getTicketCode());
                }

                lockBrokenSeat(conn, ticket, staffId, cleanReason);
                Integer voucherId = giveVoucher(conn, ticket, options);
                int exchangeId = exchangeDAO.insertExchange(conn, ticket.getTicketId(), type, newTicketIds.get(0),
                        newTicketIds.size() > 1 ? newTicketIds.get(1) : null, cleanReason, cash, points, voucherId,
                        staffId, shift == null ? null : shift.getShiftId());
                conn.commit();
                return exchangeId;
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                // Hai nhân viên đổi vào cùng ghế cùng lúc: UX_Ticket_Showtime_Seat chặn giao dịch đến sau
                if (e instanceof SQLException sql && isDuplicateKey(sql)) {
                    throw error(ERROR_TARGET, TARGET_TAKEN);
                }
                throw e;
            }
        }
    }

    // Khách chọn hủy vé (dù còn ghế để đổi): hoàn tiền mặt khách thực trả + trả lại điểm + voucher đền bù (thành viên)
    public int refund(int ticketId, String reason, int staffId) throws SQLException, ValidationException {
        String cleanReason = requireReason(reason);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ExchangeTicket ticket = exchangeDAO.findTicketById(conn, ticketId, true);
                requireExchangeable(ticket);
                ExchangeOptions options = options(conn, ticket);
                Shift shift = requireShift(conn, staffId);
                if (!exchangeDAO.closeTicket(conn, ticket.getTicketId(), TicketStatus.CANCELLED, options.refundCash,
                        shift.getShiftId())) {
                    throw error(ERROR_GENERAL, TICKET_NOT_VALID);
                }
                if (options.refundPoints > 0 && ticket.isMember()) {
                    if (!bookingDAO.addPoints(conn, ticket.getCustomerId(), options.refundPoints)) {
                        throw new SQLException("Customer loyalty balance exceeds the supported range.");
                    }
                    bookingDAO.insertLoyaltyTransaction(conn, ticket.getCustomerId(), ticket.getBookingId(),
                            "Adjustment", options.refundPoints, POINTS_NOTE_PREFIX + ticket.getTicketCode());
                }
                lockBrokenSeat(conn, ticket, staffId, cleanReason);
                Integer voucherId = giveVoucher(conn, ticket, options);
                int exchangeId = exchangeDAO.insertExchange(conn, ticket.getTicketId(), TYPE_REFUND, null, null,
                        cleanReason, options.refundCash, ticket.isMember() ? options.refundPoints : 0, voucherId,
                        staffId, shift.getShiftId());
                conn.commit();
                return exchangeId;
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public ExchangeRecord record(int exchangeId) throws SQLException {
        return exchangeDAO.findRecord(exchangeId);
    }

    // ===== Nội bộ =====

    private ExchangeOptions options(Connection conn, ExchangeTicket ticket) throws SQLException {
        ExchangeOptions options = new ExchangeOptions(ticket);
        Map<Integer, Seat> roomSeats = bookingDAO.findRoomSeats(conn, ticket.getRoomId());
        Set<Integer> taken = exchangeDAO.findTakenSeatIds(conn, ticket.getShowtimeId());
        List<Seat> free = new ArrayList<>();
        for (Seat seat : roomSeats.values()) {
            if (seat.getSeatId() != ticket.getSeatId() && !RoomStatus.MAINTENANCE.equals(seat.getStatus())
                    && !taken.contains(seat.getSeatId())) {
                free.add(seat);
            }
        }
        free.sort(Comparator.comparing(Seat::getRowName).thenComparingInt(Seat::getColNumber));

        // Hết ghế cùng hạng mới mở phương án khác hạng
        String type = ticket.getSeatType();
        addSeats(options.same, free, type);
        if (options.same.isEmpty()) {
            if (SeatType.STANDARD.equals(type)) {
                addSeats(options.upgrade, free, SeatType.VIP);
            } else if (SeatType.VIP.equals(type)) {
                addSeats(options.downgrade, free, SeatType.STANDARD);
            } else if (SeatType.SWEETBOX.equals(type)) {
                addPairs(options.pairs, free);
            }
        }

        BigDecimal surcharge = configDAO.getDecimal(conn, CONFIG_VIP_SURCHARGE, DEFAULT_VIP_SURCHARGE);
        options.vipDifference = TicketPricing.priceOf(SeatType.VIP, ticket.getBasePrice(), surcharge)
                .subtract(TicketPricing.priceOf(SeatType.STANDARD, ticket.getBasePrice(), surcharge)).max(BigDecimal.ZERO);
        options.voucherValue = configDAO.getDecimal(conn, CONFIG_VOUCHER_VALUE, DEFAULT_VOUCHER_VALUE);

        // Voucher chỉ trừ vào tiền vé; phần còn lại của đơn (sau voucher) trả bằng điểm + tiền mặt; bắp nước không hoàn.
        // shortcut: tiền voucher suy từ DiscountAmount - điểm x tỷ lệ hiện tại; Admin đổi POINT_EXCHANGE_RATE sau khi
        // bán thì lệch, cần lưu riêng tiền voucher trên Booking nếu muốn chính xác tuyệt đối
        int rate = Math.max(1, configDAO.getInt(conn, CONFIG_POINT_RATE, DEFAULT_POINT_RATE));
        BigDecimal ticketTotal = ticket.getTotalPrice().subtract(ticket.getConcessionTotal());
        BigDecimal voucher = ticket.getDiscountAmount()
                .subtract(BigDecimal.valueOf((long) ticket.getPointsRedeemed() * rate))
                .max(BigDecimal.ZERO).min(ticketTotal.max(BigDecimal.ZERO));
        // Hoàn vé ghế hỏng: hoàn cả bill (toàn bộ tiền khách thực trả + toàn bộ điểm đã dùng, kể cả phần bắp nước),
        // vé còn lại của đơn khách vẫn giữ. Trừ phần đơn đã nhận ở lần đổi/hoàn trước để một đơn chỉ được hoàn một lần
        BigDecimal[] refunded = exchangeDAO.sumRefunded(conn, ticket.getBookingId());
        options.refundCash = ticket.getFinalAmount().subtract(refunded[0]).max(BigDecimal.ZERO);
        options.refundPoints = Math.max(0, ticket.getPointsRedeemed() - refunded[1].intValue());
        Refund downgrade = refundOf(options.vipDifference.min(ticket.getPrice()), ticketTotal, voucher,
                ticket.getTotalPrice(), ticket.getPointsRedeemed(), rate);
        options.downgradePoints = downgrade.points();
        options.downgradeCash = downgrade.cash();
        return options;
    }

    // Phần khách thực trả cho tiền chênh lệch VIP khi xuống ghế thường, trả bằng gì hoàn bằng nấy (BR UC-20):
    //   giá trị = khoản tiền x (tổng tiền vé - voucher) / tổng tiền vé      (voucher chia theo giá vé)
    //   điểm    = điểm đã dùng x giá trị / (tổng đơn - voucher), làm tròn xuống
    //   tiền    = giá trị - điểm x tỷ lệ quy đổi                            (phần lẻ của điểm trả bằng tiền)
    record Refund(int points, BigDecimal cash) {
    }

    static Refund refundOf(BigDecimal amount, BigDecimal ticketTotal, BigDecimal voucher, BigDecimal orderTotal,
                           int pointsRedeemed, int rate) {
        if (amount.signum() <= 0 || ticketTotal.signum() <= 0) {
            return new Refund(0, BigDecimal.ZERO);
        }
        BigDecimal value = amount.multiply(ticketTotal.subtract(voucher)).divide(ticketTotal, 0, RoundingMode.DOWN);
        BigDecimal rest = orderTotal.subtract(voucher);
        int points = rest.signum() <= 0 ? 0 : BigDecimal.valueOf(pointsRedeemed).multiply(value)
                .divide(rest, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(pointsRedeemed)).intValue();
        BigDecimal cash = value.subtract(BigDecimal.valueOf((long) points * rate)).max(BigDecimal.ZERO);
        return new Refund(points, cash);
    }

    private void addSeats(List<ExchangeOptions.Choice> target, List<Seat> free, String seatType) {
        for (Seat seat : free) {
            if (seatType.equals(seat.getSeatType())) {
                target.add(new ExchangeOptions.Choice(String.valueOf(seat.getSeatId()),
                        seat.getRowName() + seat.getColNumber(), seatType));
            }
        }
    }

    // Hai ghế đơn trống liền kề trong cùng hàng, cùng loại (cùng VIP hoặc cùng thường), thay cho một ghế đôi
    private void addPairs(List<ExchangeOptions.Choice> target, List<Seat> free) {
        for (int i = 0; i + 1 < free.size(); i++) {
            Seat left = free.get(i);
            Seat right = free.get(i + 1);
            if (isSingle(left) && left.getSeatType().equals(right.getSeatType())
                    && left.getRowName().equals(right.getRowName())
                    && right.getColNumber() == left.getColNumber() + 1) {
                target.add(new ExchangeOptions.Choice(left.getSeatId() + "-" + right.getSeatId(),
                        left.getRowName() + left.getColNumber() + " + " + right.getRowName() + right.getColNumber(),
                        left.getSeatType()));
            }
        }
    }

    private boolean isSingle(Seat seat) {
        return SeatType.STANDARD.equals(seat.getSeatType()) || SeatType.VIP.equals(seat.getSeatType());
    }

    // Tổng giá vé mới bằng giá còn lại của vé cũ; 2 ghế thì chia đôi (phần lẻ cộng vào vé đầu)
    private List<BigDecimal> newPrices(BigDecimal amount, int count) {
        BigDecimal each = amount.divide(BigDecimal.valueOf(count), 0, RoundingMode.DOWN);
        List<BigDecimal> prices = new ArrayList<>();
        prices.add(amount.subtract(each.multiply(BigDecimal.valueOf(count - 1L))));
        for (int i = 1; i < count; i++) {
            prices.add(each);
        }
        return prices;
    }

    private List<Integer> createTickets(Connection conn, ExchangeTicket old, List<Integer> seatIds,
                                        List<BigDecimal> prices) throws SQLException {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < seatIds.size(); i++) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(old.getBookingId());
            ticket.setSeatId(seatIds.get(i));
            ticket.setPrice(prices.get(i));
            ticket.setTicketCode(newTicketCode());
            ticket.setStatus(TicketStatus.UPCOMING);
            ticket.setShowtimeId(old.getShowtimeId());
            tickets.add(ticket);
        }
        bookingDAO.insertTickets(conn, tickets);
        // Vé mới cần mã QR để soát vé; chỉ gán cho vé vừa tạo (QR còn trống), không đụng vé khác của đơn
        List<Ticket> created = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (Integer seatId : seatIds) {
            for (Ticket saved : bookingDAO.findTickets(conn, old.getBookingId())) {
                if (saved.getSeatId() == seatId && saved.getQrCode() == null
                        && TicketStatus.UPCOMING.equals(saved.getStatus())) {
                    saved.setQrCode(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
                    created.add(saved);
                    ids.add(saved.getTicketId());
                    break;
                }
            }
        }
        bookingDAO.updateTicketQrCodes(conn, created);
        return ids;
    }

    // BR: ghế hỏng phải khóa bảo trì ngay; chưa có báo cáo đang mở thì tạo để Manager xử lý ở UC-26
    private void lockBrokenSeat(Connection conn, ExchangeTicket ticket, int staffId, String reason)
            throws SQLException {
        seatDAO.updateStatus(conn, ticket.getSeatId(), RoomStatus.MAINTENANCE);
        if (exchangeDAO.findOpenSeatReport(conn, ticket.getSeatId()) != null) {
            return;
        }
        EquipmentReport report = new EquipmentReport();
        report.setStaffId(staffId);
        report.setRoomId(ticket.getRoomId());
        report.setSeatId(ticket.getSeatId());
        report.setEquipmentCategory(EquipmentCategory.SEAT);
        report.setUrgencyLevel(UrgencyLevel.MEDIUM);
        report.setDescription(REPORT_DESCRIPTION_PREFIX + ticket.getTicketCode() + "): " + reason);
        report.setStatus(ReportStatus.PENDING);
        int reportId = reportDAO.insert(conn, report, null);
        // Ghế hỏng mới có thể làm phòng vượt ngưỡng hỏng quá 50% số ghế (UC-21)
        reportService.lockRoomIfOverHalf(conn, ticket.getRoomId(), reportId, staffId);
    }

    // Voucher đền bù cá nhân, dùng một lần, không cần đơn tối thiểu; khách vãng lai không có ví nên không tặng
    private Integer giveVoucher(Connection conn, ExchangeTicket ticket, ExchangeOptions options) throws SQLException {
        if (!options.isVoucherEligible()) {
            return null;
        }
        int days = Math.max(1, configDAO.getInt(conn, CONFIG_VOUCHER_DAYS, DEFAULT_VOUCHER_DAYS));
        Voucher voucher = new Voucher();
        voucher.setVoucherCode("GH" + options.voucherValue.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN)
                + "K-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
        voucher.setDiscountValue(options.voucherValue);
        voucher.setDiscountType(VoucherService.AMOUNT);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(days));
        voucher.setOwnerUserId(ticket.getCustomerId());
        voucher.setStatus(VoucherService.ACTIVE);
        // Bảng Voucher chỉ có loại Promotion/Refund80 (UC-28); voucher đền bù là khuyến mãi cá nhân
        voucher.setVoucherType(VoucherType.PROMOTION);
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        voucherDAO.insertRefundVoucher(conn, voucher);
        return exchangeDAO.findVoucherId(conn, voucher.getVoucherCode());
    }

    private Shift requireShift(Connection conn, int staffId) throws SQLException, ValidationException {
        Shift shift = shiftDAO.findOpenByStaff(conn, staffId, true);
        if (shift == null || shiftService.isExpired(shift, shiftService.shiftHours())) {
            throw error(ERROR_GENERAL, SHIFT_REQUIRED);
        }
        return shift;
    }

    // Vé còn hiệu lực của đơn đã thanh toán; suất đang chiếu vẫn đổi được (khách phát hiện ghế hỏng khi đang xem)
    private void requireExchangeable(ExchangeTicket ticket) throws ValidationException {
        if (ticket == null) {
            throw error(ERROR_GENERAL, TICKET_NOT_FOUND);
        }
        if (!TicketStatus.UPCOMING.equals(ticket.getTicketStatus())
                || !BookingStatus.PAID.equals(ticket.getBookingStatus())) {
            throw error(ERROR_GENERAL, TICKET_NOT_VALID);
        }
        if (!ShowtimeStatus.ACTIVE.equals(ticket.getShowtimeStatus()) || ticket.getEndTime() == null
                || !ticket.getEndTime().isAfter(LocalDateTime.now())) {
            throw error(ERROR_GENERAL, SHOWTIME_ENDED);
        }
    }

    private String requireReason(String reason) throws ValidationException {
        String value = reason == null ? "" : reason.trim();
        if (value.isEmpty()) {
            throw error(ERROR_REASON, REASON_REQUIRED);
        }
        if (value.length() > MAX_REASON) {
            throw error(ERROR_REASON, Messages.format(Messages.MSG08, MAX_REASON));
        }
        return value;
    }

    private String newTicketCode() {
        StringBuilder code = new StringBuilder("TK");
        for (int i = 0; i < 10; i++) {
            code.append(TICKET_CODE_CHARS.charAt(RANDOM.nextInt(TICKET_CODE_CHARS.length())));
        }
        return code.toString();
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
