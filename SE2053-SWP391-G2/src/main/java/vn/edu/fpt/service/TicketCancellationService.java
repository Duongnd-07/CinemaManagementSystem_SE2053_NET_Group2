package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
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

                if (bookingDAO.markTicketCancelled(conn, ticketId, voucherAmount) != 1) {
                    throw new ValidationException(Map.of("general", Messages.TICKET_CANCELLATION_NOT_ALLOWED));
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
