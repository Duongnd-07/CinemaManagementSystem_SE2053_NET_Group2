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
import vn.edu.fpt.dao.VoucherDAO;
import vn.edu.fpt.model.CancellableTicket;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

public class TicketCancellationService {
    private static final int MINIMUM_CANCELLATION_HOURS = 2;
    private static final String ACTIVE = "Active";
    private static final String AMOUNT = "Amount";
    private final CustomerBookingDAO bookingDAO = new CustomerBookingDAO();
    private final BookingDAO loyaltyDAO = new BookingDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final CustomerNotificationDAO notificationDAO = new CustomerNotificationDAO();

    public CancellableTicket getForConfirmation(int ticketId, int customerId)
            throws SQLException, ValidationException {
        CancellableTicket ticket = bookingDAO.findCancellationCandidate(ticketId, customerId);
        validate(ticket);
        return ticket;
    }

    public void cancel(int ticketId, int customerId) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CancellableTicket ticket = bookingDAO.findCancellationCandidate(conn, ticketId, customerId);
                validate(ticket);

                int voucherDays = bookingDAO.findCancellationVoucherDays(conn);
                BigDecimal voucherAmount = ticket.getPrice().multiply(new BigDecimal("0.80"))
                        .setScale(2, RoundingMode.HALF_UP);
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

    private void validate(CancellableTicket ticket) throws ValidationException {
        if (ticket == null || !"Upcoming".equals(ticket.getTicketStatus())
                || !"Paid".equals(ticket.getBookingStatus()) || !"Online".equals(ticket.getChannel())
                || ticket.getShowtimeStart() == null) {
            throw new ValidationException(Map.of("general", Messages.TICKET_CANCELLATION_NOT_ALLOWED));
        }
        if (ticket.getShowtimeStart().isBefore(LocalDateTime.now().plusHours(MINIMUM_CANCELLATION_HOURS))) {
            throw new ValidationException(Map.of("general", Messages.TICKET_CANCELLATION_WINDOW));
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
