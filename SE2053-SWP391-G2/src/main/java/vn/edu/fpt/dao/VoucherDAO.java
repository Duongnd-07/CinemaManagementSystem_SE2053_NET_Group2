package vn.edu.fpt.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Voucher;
import vn.edu.fpt.model.VoucherType;
import vn.edu.fpt.util.DBContext;

public class VoucherDAO {
    private static final String SELECT_COLUMNS = "SELECT Voucher.VoucherID, Voucher.VoucherCode, "
            + "Voucher.DiscountValue, Voucher.DiscountType, Voucher.CreatedAt, Voucher.StartDate, "
            + "Voucher.ExpiryDate, Voucher.UsageLimit, Voucher.PerUserUsageLimit, "
            + "(SELECT COUNT(*) FROM Booking b WHERE b.VoucherID = Voucher.VoucherID "
            + "AND (b.Status = 'Paid' OR (b.Status = 'Pending' AND EXISTS "
            + "(SELECT 1 FROM Seat_Hold h WHERE h.SessionID = 'BOOKING-' + CAST(b.BookingID AS VARCHAR(20)) "
            + "AND h.ExpiryTime > GETDATE())))) AS UsageCount, Voucher.OwnerUserID, "
            + "CASE WHEN Voucher.Status = 'Active' AND Voucher.ExpiryDate <= GETDATE() THEN 'Expired' "
            + "WHEN Voucher.Status = 'Active' AND Voucher.StartDate > GETDATE() THEN 'Scheduled' "
            + "ELSE Voucher.Status END AS Status, Voucher.VoucherType, Voucher.MinOrderAmount, "
            + "Voucher.CreatedByUserID, Voucher.MovieID, Movie.Title AS MovieTitle "
            + "FROM Voucher LEFT JOIN Movie ON Movie.MovieID = Voucher.MovieID ";

    public List<Voucher> search(String keyword, String voucherType, int offset, int limit) throws SQLException {
        String sql = SELECT_COLUMNS + buildWhere(keyword, voucherType)
                + "ORDER BY Voucher.ExpiryDate DESC, Voucher.VoucherID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Voucher> vouchers = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilters(ps, keyword, voucherType);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vouchers.add(mapRow(rs));
                }
            }
        }
        return vouchers;
    }

    public int count(String keyword, String voucherType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Voucher " + buildWhere(keyword, voucherType);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilters(ps, keyword, voucherType);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> countByTypeAndStatus() throws SQLException {
        String sql = "SELECT VoucherType, "
                + "SUM(CASE WHEN Status = 'Active' AND StartDate <= GETDATE() "
                + "AND ExpiryDate > GETDATE() THEN 1 ELSE 0 END) AS Active, "
                + "SUM(CASE WHEN Status = 'Expired' OR (Status = 'Active' AND ExpiryDate <= GETDATE()) "
                + "THEN 1 ELSE 0 END) AS Expired, "
                + "SUM(CASE WHEN Status = 'Active' AND StartDate > GETDATE() THEN 1 ELSE 0 END) AS Scheduled "
                + "FROM Voucher GROUP BY VoucherType";
        Map<String, Integer> totals = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("VoucherType");
                totals.put(type + ":Active", rs.getInt("Active"));
                totals.put(type + ":Expired", rs.getInt("Expired"));
                totals.put(type + ":Scheduled", rs.getInt("Scheduled"));
            }
        }
        return totals;
    }

    public Voucher findById(int voucherId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE VoucherID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean update(Voucher voucher) throws SQLException {
        String sql = "UPDATE Voucher SET VoucherCode = ?, DiscountValue = ?, DiscountType = ?, "
                + "StartDate = ?, ExpiryDate = ?, UsageLimit = ?, Status = ?, VoucherType = ?, "
                + "MinOrderAmount = ?, CreatedByUserID = ?, MovieID = ?, PerUserUsageLimit = ? "
                + "WHERE VoucherID = ? AND ? >= (SELECT COUNT(*) FROM Booking b "
                + "WHERE b.VoucherID = Voucher.VoucherID AND (b.Status = 'Paid' OR "
                + "(b.Status = 'Pending' AND EXISTS (SELECT 1 FROM Seat_Hold h "
                + "WHERE h.SessionID = 'BOOKING-' + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE())))) AND NOT EXISTS (SELECT 1 FROM Booking b "
                + "WHERE b.VoucherID = Voucher.VoucherID AND b.CustomerID IS NOT NULL AND "
                + "(b.Status = 'Paid' OR (b.Status = 'Pending' AND EXISTS (SELECT 1 FROM Seat_Hold h "
                + "WHERE h.SessionID = 'BOOKING-' + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE()))) GROUP BY b.CustomerID HAVING COUNT(*) > ?)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucher.getVoucherCode());
            ps.setBigDecimal(2, voucher.getDiscountValue());
            ps.setString(3, voucher.getDiscountType());
            ps.setTimestamp(4, Timestamp.valueOf(voucher.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(voucher.getExpiryDate()));
            ps.setInt(6, voucher.getUsageLimit());
            ps.setString(7, voucher.getStatus());
            ps.setString(8, voucher.getVoucherType());
            ps.setBigDecimal(9, voucher.getMinOrderAmount());
            if (voucher.getCreatedByUserId() == null) {
                ps.setNull(10, java.sql.Types.INTEGER);
            } else {
                ps.setInt(10, voucher.getCreatedByUserId());
            }
            if (voucher.getMovieId() == null) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setInt(11, voucher.getMovieId());
            }
            ps.setInt(12, voucher.getPerUserUsageLimit());
            ps.setInt(13, voucher.getVoucherId());
            ps.setInt(14, voucher.getUsageLimit());
            ps.setInt(15, voucher.getPerUserUsageLimit());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int voucherId) throws SQLException {
        String sql = "DELETE FROM Voucher WHERE VoucherID = ? AND VoucherType = ? "
                + "AND NOT EXISTS (SELECT 1 FROM Booking WHERE Booking.VoucherID = Voucher.VoucherID)";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setString(2, VoucherType.PROMOTION);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Voucher> findWalletForOwner(int ownerUserId) throws SQLException {
        String sql = SELECT_COLUMNS
                + "WHERE Voucher.OwnerUserID = ? ORDER BY CASE WHEN Voucher.Status = 'Active' THEN 0 "
                + "WHEN Voucher.Status = 'Used' THEN 1 ELSE 2 END, Voucher.ExpiryDate DESC, Voucher.VoucherID DESC";
        List<Voucher> vouchers = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vouchers.add(mapRow(rs));
                }
            }
        }
        return vouchers;
    }

    public void insertRefundVoucher(Connection conn, Voucher voucher) throws SQLException {
        String sql = "INSERT INTO Voucher (VoucherCode, DiscountValue, DiscountType, StartDate, ExpiryDate, "
                + "OwnerUserID, Status, VoucherType, MinOrderAmount, CreatedByUserID) "
                + "VALUES (?, ?, ?, GETDATE(), ?, ?, ?, ?, ?, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucher.getVoucherCode());
            ps.setBigDecimal(2, voucher.getDiscountValue());
            ps.setString(3, voucher.getDiscountType());
            ps.setTimestamp(4, Timestamp.valueOf(voucher.getExpiryDate()));
            ps.setInt(5, voucher.getOwnerUserId());
            ps.setString(6, voucher.getStatus());
            ps.setString(7, voucher.getVoucherType());
            ps.setBigDecimal(8, voucher.getMinOrderAmount());
            ps.executeUpdate();
        }
    }

    public boolean existsByCode(String voucherCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Voucher WHERE VoucherCode = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, voucherCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int countActiveUses(int voucherId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Booking b WHERE b.VoucherID = ? AND "
                + "(b.Status = 'Paid' OR (b.Status = 'Pending' AND EXISTS "
                + "(SELECT 1 FROM Seat_Hold h WHERE h.SessionID = 'BOOKING-' + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE())))";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int maxActiveUsesPerUser(int voucherId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(UsageCount), 0) FROM ("
                + "SELECT COUNT(*) AS UsageCount FROM Booking b WHERE b.VoucherID = ? "
                + "AND b.CustomerID IS NOT NULL AND (b.Status = 'Paid' OR "
                + "(b.Status = 'Pending' AND EXISTS (SELECT 1 FROM Seat_Hold h "
                + "WHERE h.SessionID = 'BOOKING-' + CAST(b.BookingID AS VARCHAR(20)) "
                + "AND h.ExpiryTime > GETDATE()))) GROUP BY b.CustomerID) activeUses";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int insertPromotion(Voucher voucher) throws SQLException {
        String sql = "INSERT INTO Voucher (VoucherCode, DiscountValue, DiscountType, StartDate, ExpiryDate, "
                + "UsageLimit, OwnerUserID, Status, VoucherType, MinOrderAmount, CreatedByUserID, MovieID, "
                + "PerUserUsageLimit) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, voucher.getVoucherCode());
            ps.setBigDecimal(2, voucher.getDiscountValue());
            ps.setString(3, voucher.getDiscountType());
            ps.setTimestamp(4, Timestamp.valueOf(voucher.getStartDate()));
            ps.setTimestamp(5, Timestamp.valueOf(voucher.getExpiryDate()));
            ps.setInt(6, voucher.getUsageLimit());
            ps.setString(7, voucher.getStatus());
            ps.setString(8, voucher.getVoucherType());
            ps.setBigDecimal(9, voucher.getMinOrderAmount());
            if (voucher.getCreatedByUserId() == null) {
                ps.setNull(10, java.sql.Types.INTEGER);
            } else {
                ps.setInt(10, voucher.getCreatedByUserId());
            }
            if (voucher.getMovieId() == null) {
                ps.setNull(11, java.sql.Types.INTEGER);
            } else {
                ps.setInt(11, voucher.getMovieId());
            }
            ps.setInt(12, voucher.getPerUserUsageLimit());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private String buildWhere(String keyword, String voucherType) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            conditions.add("Voucher.VoucherCode LIKE ?");
        }
        if (voucherType != null && !voucherType.isEmpty()) {
            conditions.add("Voucher.VoucherType = ?");
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private int bindFilters(PreparedStatement ps, String keyword, String voucherType) throws SQLException {
        int idx = 1;
        if (keyword != null && !keyword.isEmpty()) {
            ps.setString(idx++, "%" + keyword + "%");
        }
        if (voucherType != null && !voucherType.isEmpty()) {
            ps.setString(idx++, voucherType);
        }
        return idx;
    }

    private Voucher mapRow(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(rs.getInt("VoucherID"));
        voucher.setVoucherCode(rs.getString("VoucherCode"));
        voucher.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        voucher.setDiscountType(rs.getString("DiscountType"));
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        voucher.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        Timestamp startDate = rs.getTimestamp("StartDate");
        voucher.setStartDate(startDate == null ? null : startDate.toLocalDateTime());
        Timestamp expiryDate = rs.getTimestamp("ExpiryDate");
        voucher.setExpiryDate(expiryDate == null ? null : expiryDate.toLocalDateTime());
        voucher.setUsageLimit(rs.getObject("UsageLimit", Integer.class));
        voucher.setPerUserUsageLimit(rs.getObject("PerUserUsageLimit", Integer.class));
        voucher.setUsageCount(rs.getInt("UsageCount"));
        voucher.setOwnerUserId(rs.getObject("OwnerUserID", Integer.class));
        voucher.setStatus(rs.getString("Status"));
        voucher.setVoucherType(rs.getString("VoucherType"));
        BigDecimal minOrderAmount = rs.getBigDecimal("MinOrderAmount");
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setCreatedByUserId(rs.getObject("CreatedByUserID", Integer.class));
        voucher.setMovieId(rs.getObject("MovieID", Integer.class));
        voucher.setMovieTitle(rs.getString("MovieTitle"));
        return voucher;
    }
}
