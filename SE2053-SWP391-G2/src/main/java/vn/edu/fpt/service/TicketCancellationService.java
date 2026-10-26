package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import vn.edu.fpt.dao.BookingDAO;
import vn.edu.fpt.dao.CustomerBookingDAO;
import vn.edu.fpt.dao.CustomerNotificationDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.CancellableTicket;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

public class TicketCancellationService {
    // UC-33: hạn hủy vé và % giá vé hoàn thành voucher do Admin cấu hình
    private static final String CONFIG_MIN_HOURS = "CANCELLATION_MIN_HOURS";
    private static final String CONFIG_VOUCHER_PERCENT = "CANCELLATION_VOUCHER_PERCENT";
    private static final int DEFAULT_MIN_HOURS = 24;
    private static final int DEFAULT_VOUCHER_PERCENT = 80;
    private static final String ACTIVE = "Active";
    private static final String AMOUNT = "Amount";
    private final CustomerBookingDAO bookingDAO = new CustomerBookingDAO();
    private final BookingDAO loyaltyDAO = new BookingDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();

    public CancellableTicket getForConfirmation(int ticketId, int customerId)
            throws SQLException, ValidationException {
        CancellableTicket ticket = bookingDAO.findCancellationCandidate(ticketId, customerId);
        validate(ticket, minimumHours());
        return ticket;
    }

    public int minimumHours() throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return minimumHours(conn);
        }
    }

    public int voucherPercent() throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return voucherPercent(conn);
        }
    }

    // Trả về % giá vé đã hoàn thành voucher để báo lại cho khách
    public int cancel(int ticketId, int customerId) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CancellableTicket ticket = bookingDAO.findCancellationCandidate(conn, ticketId, customerId);
                validate(ticket, minimumHours(conn));

                int voucherDays = bookingDAO.findCancellationVoucherDays(conn);
                int voucherPercent = voucherPercent(conn);
                BigDecimal voucherAmount = ticket.getPrice().multiply(BigDecimal.valueOf(voucherPercent))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                Voucher voucher = createRefundVoucher(customerId, voucherAmount, voucherDays);
                int pointsToRevoke = pointsToRevoke(conn, ticket);

                if (bookingDAO.markTicketCancelled(conn, ticketId, voucherAmount) != 1) {
                    throw new ValidationException(Map.of("general", Messages.TICKET_CANCELLATION_NOT_ALLOWED));
                }
                if (pointsToRevoke > 0) {
                    if (!loyaltyDAO.revokeEarnedPoints(conn, customerId, pointsToRevoke)) {
                        throw new SQLException("Customer loyalty balance is outside the supported range.");
                    }
                    loyaltyDAO.insertLoyaltyTransaction(conn, customerId, ticket.getBookingId(), "Adjustment",
                            -pointsToRevoke, "Thu hồi điểm do hủy vé UC16 #" + ticketId);
                }
                voucherDAO.insertRefundVoucher(conn, voucher);
                notificationDAO.insert(conn, customerId, "Voucher hoàn tiền vé",
                        "Vé đã được hủy. Voucher " + voucher.getVoucherCode()
                                + " trị giá " + voucherAmount.toPlainString() + "đ đã được thêm vào ví.",
                        "Refund");
                conn.commit();
                return voucherPercent;
            } catch (SQLException | ValidationException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    private int pointsToRevoke(Connection conn, CancellableTicket ticket) throws SQLException {
        int earnedPoints = ticket.getBookingPointsEarned();
        if (earnedPoints <= 0) {
            return 0;
        }

        long alreadyRevoked = loyaltyDAO.sumTicketCancellationPointReversals(conn, ticket.getBookingId());
        int remainingEarnedPoints = (int) Math.max(0L, earnedPoints - alreadyRevoked);
        if (remainingEarnedPoints == 0) {
            return 0;
        }
        if (ticket.getUpcomingTicketCount() <= 1) {
            return remainingEarnedPoints;
        }

        BigDecimal totalTicketPrice = ticket.getBookingTicketTotal();
        if (totalTicketPrice == null || totalTicketPrice.signum() <= 0) {
            throw new SQLException("Booking ticket total must be positive to reverse earned loyalty points.");
        }
        int proportionalPoints = BigDecimal.valueOf(earnedPoints).multiply(ticket.getPrice())
                .divide(totalTicketPrice, 0, RoundingMode.DOWN).intValueExact();
        return Math.min(proportionalPoints, remainingEarnedPoints);
    }

    private int minimumHours(Connection conn) throws SQLException {
        int hours = configDAO.getInt(conn, CONFIG_MIN_HOURS, DEFAULT_MIN_HOURS);
        if (hours < 0) {
            throw new SQLException("CANCELLATION_MIN_HOURS must not be negative.");
        }
        return hours;
    }

    private int voucherPercent(Connection conn) throws SQLException {
        int percent = configDAO.getInt(conn, CONFIG_VOUCHER_PERCENT, DEFAULT_VOUCHER_PERCENT);
        if (percent < 1 || percent > 100) {
            throw new SQLException("CANCELLATION_VOUCHER_PERCENT must be between 1 and 100.");
        }
        return percent;
    }

    private void validate(CancellableTicket ticket, int minimumHours) throws ValidationException {
        if (ticket == null || !"Upcoming".equals(ticket.getTicketStatus())
                || !"Paid".equals(ticket.getBookingStatus()) || !"Online".equals(ticket.getChannel())
                || ticket.getShowtimeStart() == null) {
            throw new ValidationException(Map.of("general", Messages.TICKET_CANCELLATION_NOT_ALLOWED));
        }
        if (ticket.getShowtimeStart().isBefore(LocalDateTime.now().plusHours(minimumHours))) {
            throw new ValidationException(Map.of("general",
                    Messages.format(Messages.TICKET_CANCELLATION_WINDOW, String.valueOf(minimumHours))));
        }
    }

    private Voucher createRefundVoucher(int customerId, BigDecimal voucherAmount, int voucherDays) {
        Voucher voucher = new Voucher();
        voucher.setVoucherCode("RF80-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                .toUpperCase());
        voucher.setDiscountValue(voucherAmount);
        voucher.setDiscountType(AMOUNT);
        voucher.setExpiryDate(LocalDateTime.now().plusDays(voucherDays));
        voucher.setOwnerUserId(customerId);
        voucher.setStatus(ACTIVE);
        voucher.setVoucherType(VoucherType.REFUND_80);
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        return voucher;
    }
}
