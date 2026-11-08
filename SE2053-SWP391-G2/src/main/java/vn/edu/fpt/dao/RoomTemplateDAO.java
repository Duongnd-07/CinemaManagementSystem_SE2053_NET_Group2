package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.RoomTemplate;
import vn.edu.fpt.util.DBContext;

// UC-25 (A3-A5): đọc/ghi bảng RoomTemplate (không liên quan bảng Room)
public class RoomTemplateDAO {
    private static final String COLUMNS = "SELECT TemplateID, TemplateName, Format, GridRows, GridCols, Layout, UpdatedAt "
            + "FROM RoomTemplate ";

    public List<RoomTemplate> findAll() throws SQLException {
        List<RoomTemplate> templates = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(COLUMNS + "ORDER BY TemplateName");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                templates.add(map(rs));
            }
        }
        return templates;
    }

    public RoomTemplate findById(int templateId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(COLUMNS + "WHERE TemplateID = ?")) {
            ps.setInt(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // So sánh theo collation của cột (CI) để khớp đúng ràng buộc UNIQUE(TemplateName)
    public boolean existsByName(String name, int excludeTemplateId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM RoomTemplate WHERE TemplateName = ? AND TemplateID <> ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeTemplateId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int insert(RoomTemplate template) throws SQLException {
        String sql = "INSERT INTO RoomTemplate (TemplateName, Format, GridRows, GridCols, Layout) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, template);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Không lấy được TemplateID vừa tạo");
                }
                return keys.getInt(1);
            }
        }
    }

    // false nếu phòng mẫu đã bị xóa ở phiên khác (E6)
    public boolean update(RoomTemplate template) throws SQLException {
        String sql = "UPDATE RoomTemplate SET TemplateName = ?, Format = ?, GridRows = ?, GridCols = ?, Layout = ?, "
                + "UpdatedAt = GETDATE() WHERE TemplateID = ?";
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, template);
            ps.setInt(6, template.getTemplateId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int templateId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM RoomTemplate WHERE TemplateID = ?")) {
            ps.setInt(1, templateId);
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, RoomTemplate template) throws SQLException {
        ps.setString(1, template.getTemplateName());
        ps.setString(2, template.getFormat());
        ps.setInt(3, template.getRows());
        ps.setInt(4, template.getCols());
        ps.setString(5, template.getLayout());
    }

    private RoomTemplate map(ResultSet rs) throws SQLException {
        RoomTemplate template = new RoomTemplate();
        template.setTemplateId(rs.getInt("TemplateID"));
        template.setTemplateName(rs.getString("TemplateName"));
        template.setFormat(rs.getString("Format"));
        template.setRows(rs.getInt("GridRows"));
        template.setCols(rs.getInt("GridCols"));
        template.setLayout(rs.getString("Layout"));
        Timestamp updated = rs.getTimestamp("UpdatedAt");
        template.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return template;
    }
}
