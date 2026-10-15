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
    private static final String SELECT_COLUMNS = "SELECT VoucherID, VoucherCode, DiscountValue, DiscountType, "
            + "ExpiryDate, OwnerUserID, CASE WHEN Status = 'Active' AND ExpiryDate <= GETDATE() "
            + "THEN 'Expired' ELSE Status END AS Status, VoucherType, MinOrderAmount, CreatedByUserID FROM Voucher ";

    public List<Voucher> search(String keyword, String voucherType, int offset, int limit) throws SQLException {
        String sql = SELECT_COLUMNS + buildWhere(keyword, voucherType)
                + "ORDER BY ExpiryDate DESC, VoucherID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
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

    public Map<String, Integer> countRefundInventory() throws SQLException {
        String effectiveStatus = "CASE WHEN Status = 'Active' AND ExpiryDate <= GETDATE() "
                + "THEN 'Expired' ELSE Status END";
        String sql = "SELECT " + effectiveStatus + " AS Status, COUNT(*) AS Total FROM Voucher "
                + "WHERE VoucherType = ? GROUP BY " + effectiveStatus;
        Map<String, Integer> totals = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, VoucherType.REFUND_80);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return totals;
    }

    public List<Voucher> findWalletForOwner(int ownerUserId) throws SQLException {
        String sql = SELECT_COLUMNS
                + "WHERE OwnerUserID = ? ORDER BY CASE WHEN Status = 'Active' THEN 0 "
                + "WHEN Status = 'Used' THEN 1 ELSE 2 END, ExpiryDate DESC, VoucherID DESC";
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
        String sql = "INSERT INTO Voucher (VoucherCode, DiscountValue, DiscountType, ExpiryDate, OwnerUserID, "
                + "Status, VoucherType, MinOrderAmount, CreatedByUserID) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)";
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

    public int insertPromotion(Voucher voucher) throws SQLException {
        String sql = "INSERT INTO Voucher (VoucherCode, DiscountValue, DiscountType, ExpiryDate, OwnerUserID, "
                + "Status, VoucherType, MinOrderAmount, CreatedByUserID) VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, voucher.getVoucherCode());
            ps.setBigDecimal(2, voucher.getDiscountValue());
            ps.setString(3, voucher.getDiscountType());
            ps.setTimestamp(4, Timestamp.valueOf(voucher.getExpiryDate()));
            ps.setString(5, voucher.getStatus());
            ps.setString(6, voucher.getVoucherType());
            ps.setBigDecimal(7, voucher.getMinOrderAmount());
            if (voucher.getCreatedByUserId() == null) {
                ps.setNull(8, java.sql.Types.INTEGER);
            } else {
                ps.setInt(8, voucher.getCreatedByUserId());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private String buildWhere(String keyword, String voucherType) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            conditions.add("VoucherCode LIKE ?");
        }
        if (voucherType != null && !voucherType.isEmpty()) {
            conditions.add("VoucherType = ?");
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
        Timestamp expiryDate = rs.getTimestamp("ExpiryDate");
        voucher.setExpiryDate(expiryDate == null ? null : expiryDate.toLocalDateTime());
        voucher.setOwnerUserId(rs.getObject("OwnerUserID", Integer.class));
        voucher.setStatus(rs.getString("Status"));
        voucher.setVoucherType(rs.getString("VoucherType"));
        BigDecimal minOrderAmount = rs.getBigDecimal("MinOrderAmount");
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setCreatedByUserId(rs.getObject("CreatedByUserID", Integer.class));
        return voucher;
    }
}
