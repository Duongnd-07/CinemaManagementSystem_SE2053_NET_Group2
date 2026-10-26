package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import vn.edu.fpt.model.PaymentAttempt;
import vn.edu.fpt.model.PaymentMethod;

public class PaymentDAO {
    // Trạng thái lần thanh toán gần nhất của đơn (null nếu khách chưa bấm thanh toán)
    public String findLatestStatusForBooking(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT TOP 1 Status FROM OnlinePayment WHERE BookingID = ? ORDER BY OnlinePaymentID DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void expireInitiatedForBooking(Connection conn, int bookingId) throws SQLException {
        String sql = "UPDATE OnlinePayment SET Status = ? WHERE BookingID = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentAttempt.STATUS_EXPIRED);
            ps.setInt(2, bookingId);
            ps.setString(3, PaymentAttempt.STATUS_INITIATED);
            ps.executeUpdate();
        }
    }

    public void insertInitiated(Connection conn, int bookingId, String reference, BigDecimal amount)
            throws SQLException {
        String sql = "INSERT INTO OnlinePayment (BookingID, Provider, ProviderTxnRef, Amount, Status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setString(2, PaymentMethod.VNPAY);
            ps.setString(3, reference);
            ps.setBigDecimal(4, amount);
            ps.setString(5, PaymentAttempt.STATUS_INITIATED);
            ps.executeUpdate();
        }
    }

    public PaymentAttempt findByReference(Connection conn, String reference, boolean lock) throws SQLException {
        String sql = "SELECT p.BookingID, b.CustomerID, p.ProviderTxnRef, p.ProviderTransactionID, "
                + "p.Amount, p.ReceivedAmount, p.Status, p.ResponseCode FROM OnlinePayment p "
                + (lock ? "WITH (UPDLOCK, ROWLOCK) " : "")
                + "JOIN Booking b ON b.BookingID = p.BookingID "
                + "WHERE p.Provider = ? AND p.ProviderTxnRef = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentMethod.VNPAY);
            ps.setString(2, reference);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                PaymentAttempt attempt = new PaymentAttempt();
                attempt.setBookingId(rs.getInt("BookingID"));
                attempt.setCustomerId(rs.getInt("CustomerID"));
                attempt.setTransactionReference(rs.getString("ProviderTxnRef"));
                attempt.setProviderTransactionId(rs.getString("ProviderTransactionID"));
                attempt.setAmount(rs.getBigDecimal("Amount"));
                attempt.setReceivedAmount(rs.getBigDecimal("ReceivedAmount"));
                attempt.setStatus(rs.getString("Status"));
                attempt.setResponseCode(rs.getString("ResponseCode"));
                return attempt;
            }
        }
    }

    public boolean markPaid(Connection conn, String reference, String providerTransactionId,
                            BigDecimal receivedAmount, String responseCode, java.time.LocalDateTime paidAt)
            throws SQLException {
        String sql = "UPDATE OnlinePayment SET Status = ?, ProviderTransactionID = ?, ReceivedAmount = ?, "
                + "ResponseCode = ?, PaidAt = ? WHERE Provider = ? AND ProviderTxnRef = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentAttempt.STATUS_PAID);
            ps.setString(2, providerTransactionId);
            ps.setBigDecimal(3, receivedAmount);
            ps.setString(4, responseCode);
            ps.setTimestamp(5, Timestamp.valueOf(paidAt));
            ps.setString(6, PaymentMethod.VNPAY);
            ps.setString(7, reference);
            ps.setString(8, PaymentAttempt.STATUS_INITIATED);
            return ps.executeUpdate() == 1;
        }
    }

    public void markFailed(Connection conn, String reference, String providerTransactionId, BigDecimal receivedAmount,
                           String responseCode) throws SQLException {
        String sql = "UPDATE OnlinePayment SET Status = ?, ProviderTransactionID = ?, ReceivedAmount = ?, "
                + "ResponseCode = ? "
                + "WHERE Provider = ? AND ProviderTxnRef = ? AND Status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentAttempt.STATUS_FAILED);
            if (providerTransactionId == null || providerTransactionId.isBlank()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, providerTransactionId);
            }
            if (receivedAmount == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setBigDecimal(3, receivedAmount);
            }
            if (responseCode == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, responseCode);
            }
            ps.setString(5, PaymentMethod.VNPAY);
            ps.setString(6, reference);
            ps.setString(7, PaymentAttempt.STATUS_INITIATED);
            ps.executeUpdate();
        }
    }

    public boolean markReviewRequired(Connection conn, String reference, String providerTransactionId,
                                      BigDecimal receivedAmount, String responseCode) throws SQLException {
        String sql = "UPDATE OnlinePayment SET Status = ?, ProviderTransactionID = ?, ReceivedAmount = ?, "
                + "ResponseCode = ? WHERE Provider = ? AND ProviderTxnRef = ? AND Status IN (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PaymentAttempt.STATUS_REVIEW_REQUIRED);
            if (providerTransactionId == null || providerTransactionId.isBlank()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, providerTransactionId);
            }
            if (receivedAmount == null) {
                ps.setNull(3, Types.DECIMAL);
            } else {
                ps.setBigDecimal(3, receivedAmount);
            }
            ps.setString(4, responseCode);
            ps.setString(5, PaymentMethod.VNPAY);
            ps.setString(6, reference);
            ps.setString(7, PaymentAttempt.STATUS_INITIATED);
            ps.setString(8, PaymentAttempt.STATUS_EXPIRED);
            ps.setString(9, PaymentAttempt.STATUS_FAILED);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean hasMatchingPaidTransaction(Connection conn, PaymentAttempt attempt, String providerTransactionId,
                                              BigDecimal amount) throws SQLException {
        return PaymentAttempt.STATUS_PAID.equals(attempt.getStatus())
                && attempt.getProviderTransactionId() != null
                && attempt.getProviderTransactionId().equals(providerTransactionId)
                && attempt.getAmount().compareTo(amount) == 0
                && attempt.getReceivedAmount() != null
                && attempt.getReceivedAmount().compareTo(amount) == 0;
    }

    public boolean hasMatchingFailedCallback(PaymentAttempt attempt, String providerTransactionId,
                                             String responseCode, BigDecimal amount) {
        String storedTransactionId = attempt.getProviderTransactionId();
        String receivedTransactionId = providerTransactionId == null || providerTransactionId.isBlank()
                ? null : providerTransactionId;
        return PaymentAttempt.STATUS_FAILED.equals(attempt.getStatus())
                && java.util.Objects.equals(storedTransactionId, receivedTransactionId)
                && java.util.Objects.equals(attempt.getResponseCode(), responseCode)
                && attempt.getReceivedAmount() != null
                && attempt.getReceivedAmount().compareTo(amount) == 0;
    }

    public boolean hasMatchingReviewCallback(PaymentAttempt attempt, String providerTransactionId,
                                             String responseCode, BigDecimal amount) {
        String storedTransactionId = attempt.getProviderTransactionId();
        String receivedTransactionId = providerTransactionId == null || providerTransactionId.isBlank()
                ? null : providerTransactionId;
        return PaymentAttempt.STATUS_REVIEW_REQUIRED.equals(attempt.getStatus())
                && java.util.Objects.equals(storedTransactionId, receivedTransactionId)
                && java.util.Objects.equals(attempt.getResponseCode(), responseCode)
                && attempt.getReceivedAmount() != null
                && attempt.getReceivedAmount().compareTo(amount) == 0;
    }
}
