package vn.edu.fpt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.model.EquipmentReportLog;
import vn.edu.fpt.util.DBContext;

public class EquipmentReportLogDAO {

    public void insert(Connection conn, EquipmentReportLog log) throws SQLException {
        String sql = "INSERT INTO EquipmentReportLog (ReportID, ManagerID, OldStatus, NewStatus, Note, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getReportId());
            ps.setInt(2, log.getManagerId());
            ps.setString(3, log.getOldStatus());
            ps.setString(4, log.getNewStatus());
            ps.setString(5, log.getNote());
            ps.executeUpdate();
        }
    }

    // Mới nhất đứng trước
    public List<EquipmentReportLog> findByReport(int reportId) throws SQLException {
        String sql = "SELECT l.LogID, l.ReportID, l.ManagerID, l.OldStatus, l.NewStatus, l.Note, l.CreatedAt, "
                + "u.FullName AS ManagerName "
                + "FROM EquipmentReportLog l LEFT JOIN [User] u ON u.UserID = l.ManagerID "
                + "WHERE l.ReportID = ? ORDER BY l.CreatedAt DESC, l.LogID DESC";
        List<EquipmentReportLog> logs = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EquipmentReportLog log = new EquipmentReportLog();
                    log.setLogId(rs.getInt("LogID"));
                    log.setReportId(rs.getInt("ReportID"));
                    log.setManagerId(rs.getInt("ManagerID"));
                    log.setOldStatus(rs.getString("OldStatus"));
                    log.setNewStatus(rs.getString("NewStatus"));
                    log.setNote(rs.getString("Note"));
                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    log.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
                    log.setManagerName(rs.getString("ManagerName"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }
}
