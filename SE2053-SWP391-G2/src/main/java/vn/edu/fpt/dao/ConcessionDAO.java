package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Concession;
import vn.edu.fpt.model.ConcessionStatus;
import vn.edu.fpt.util.DBContext;

public class ConcessionDAO {

    private static final String SELECT_COLUMNS = "SELECT ConcessionID, Name, Description, Price, StockQuantity, "
            + "ImageUrl, Status FROM Concession ";

    public List<Concession> search(String keyword, String status, int offset, int limit) throws SQLException {
        String sql = SELECT_COLUMNS + buildWhere(keyword, status)
                + "ORDER BY ConcessionID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Concession> concessions = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilter(ps, keyword, status);
            ps.setInt(idx++, offset);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    concessions.add(mapRow(rs));
                }
            }
        }
        return concessions;
    }

    public int count(String keyword, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Concession " + buildWhere(keyword, status);
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, keyword, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Map<String, Integer> countByStatus(String keyword) throws SQLException {
        String sql = "SELECT Status, COUNT(*) AS Total FROM Concession " + buildWhere(keyword, null)
                + "GROUP BY Status";
        Map<String, Integer> result = new HashMap<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, keyword, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("Status"), rs.getInt("Total"));
                }
            }
        }
        return result;
    }

    public Concession findById(int concessionId) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE ConcessionID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, concessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // UC-19: chỉ những món đang bán và còn hàng
    public List<Concession> findAvailableForSale() throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE Status = ? AND StockQuantity > 0 ORDER BY Name";
        List<Concession> concessions = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ConcessionStatus.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    concessions.add(mapRow(rs));
                }
            }
        }
        return concessions;
    }

    public boolean existsByName(String name, int excludeConcessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Concession WHERE LOWER(Name) = LOWER(?) AND ConcessionID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeConcessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int insert(Concession concession) throws SQLException {
        String sql = "INSERT INTO Concession (Name, Description, Price, StockQuantity, ImageUrl, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindConcession(ps, concession);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Concession concession) throws SQLException {
        String sql = "UPDATE Concession SET Name = ?, Description = ?, Price = ?, StockQuantity = ?, ImageUrl = ?, "
                + "Status = ? WHERE ConcessionID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindConcession(ps, concession);
            ps.setInt(idx, concession.getConcessionId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int concessionId, String status) throws SQLException {
        String sql = "UPDATE Concession SET Status = ? WHERE ConcessionID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, concessionId);
            ps.executeUpdate();
        }
    }

    private String buildWhere(String keyword, String status) {
        List<String> conditions = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            conditions.add("Name LIKE ?");
        }
        if (status != null && !status.isEmpty()) {
            conditions.add("Status = ?");
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + " ";
    }

    private int bindFilter(PreparedStatement ps, String keyword, String status) throws SQLException {
        int idx = 1;
        if (keyword != null && !keyword.isEmpty()) {
            ps.setString(idx++, "%" + keyword + "%");
        }
        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }
        return idx;
    }

    private int bindConcession(PreparedStatement ps, Concession concession) throws SQLException {
        int idx = 1;
        ps.setString(idx++, concession.getName());
        ps.setString(idx++, concession.getDescription());
        ps.setBigDecimal(idx++, concession.getPrice());
        ps.setInt(idx++, concession.getStockQuantity());
        ps.setString(idx++, concession.getImageUrl());
        ps.setString(idx++, concession.getStatus());
        return idx;
    }

    private Concession mapRow(ResultSet rs) throws SQLException {
        Concession concession = new Concession();
        concession.setConcessionId(rs.getInt("ConcessionID"));
        concession.setName(rs.getString("Name"));
        concession.setDescription(rs.getString("Description"));
        concession.setPrice(rs.getBigDecimal("Price"));
        concession.setStockQuantity(rs.getInt("StockQuantity"));
        concession.setImageUrl(rs.getString("ImageUrl"));
        concession.setStatus(rs.getString("Status"));
        return concession;
    }
}
