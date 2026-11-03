package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.LoyaltyTransaction;
import vn.edu.fpt.util.DBContext;

public class LoyaltyDAO {
    public Integer findBalance(int userId) throws SQLException {
        String sql = "SELECT PointBalance FROM [User] WHERE UserID = ? AND Role = 'Customer' AND Status = 'Active'";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("PointBalance") : null;
            }
        }
    }

    public int countTransactions(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM LoyaltyPointHistory WHERE UserID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<LoyaltyTransaction> findTransactions(int userId, int offset, int limit) throws SQLException {
        String sql = "SELECT BookingID, TransactionType, PointsChange, CreatedAt FROM LoyaltyPointHistory "
                + "WHERE UserID = ? ORDER BY CreatedAt DESC, LoyaltyPointHistoryID DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<LoyaltyTransaction> transactions = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, offset);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoyaltyTransaction transaction = new LoyaltyTransaction();
                    transaction.setBookingId(rs.getObject("BookingID", Integer.class));
                    transaction.setTransactionType(rs.getString("TransactionType"));
                    transaction.setPoints(rs.getInt("PointsChange"));
                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    transaction.setTransactionDate(createdAt == null ? null : createdAt.toLocalDateTime());
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }
}
