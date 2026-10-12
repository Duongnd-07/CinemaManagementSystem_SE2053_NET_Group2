package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.EquipmentReportDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.SeatDAO;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.UrgencyLevel;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

public class EquipmentReportService {
    public static final int PAGE_SIZE = 10;
    private static final int MAX_DESCRIPTION = 1000;

    private final EquipmentReportDAO reportDAO = new EquipmentReportDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SeatDAO seatDAO = new SeatDAO();

    public Page<EquipmentReport> listByStaff(int staffId, String status, int page) throws SQLException {
        String statusFilter = ReportStatus.isValid(status) ? status : null;
        return Page.of(page, PAGE_SIZE, reportDAO.countByStaff(staffId, statusFilter),
                (offset, limit) -> reportDAO.searchByStaff(staffId, statusFilter, offset, limit));
    }

    public Map<String, Integer> countByStatus(int staffId) throws SQLException {
        return reportDAO.countByStatusForStaff(staffId);
    }

    public List<Room> listRooms() throws SQLException {
        return roomDAO.findAll();
    }

    public List<Seat> listSeats() throws SQLException {
        return seatDAO.findAll();
    }

    // E1: trả về toàn bộ lỗi theo từng trường; map rỗng nghĩa là hợp lệ
    public Map<String, String> validate(EquipmentReportForm form) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        if (form.getCategory().isEmpty()) {
            errors.put("category", Messages.format(Messages.MSG02, "Loại thiết bị"));
        } else if (!EquipmentCategory.isValid(form.getCategory())) {
            errors.put("category", Messages.REPORT_CATEGORY_INVALID);
        }

        int roomId = parseId(form.getRoomId());
        if (form.getRoomId().isEmpty()) {
            errors.put("roomId", Messages.format(Messages.MSG02, "Phòng chiếu"));
        } else if (!roomExists(roomId)) {
            errors.put("roomId", Messages.REPORT_ROOM_INVALID);
        }

        if (EquipmentCategory.SEAT.equals(form.getCategory())) {
            if (form.getSeatId().isEmpty()) {
                errors.put("seatId", Messages.format(Messages.MSG02, "Ghế"));
            } else {
                Seat seat = seatDAO.findById(parseId(form.getSeatId()));
                if (seat == null || seat.getRoomId() != roomId) {
                    errors.put("seatId", Messages.REPORT_SEAT_INVALID);
                }
            }
        }

        if (form.getUrgency().isEmpty()) {
            errors.put("urgency", Messages.format(Messages.MSG02, "Mức khẩn cấp"));
        } else if (!UrgencyLevel.isValid(form.getUrgency())) {
            errors.put("urgency", Messages.REPORT_URGENCY_INVALID);
        }

        String description = form.getDescription().trim();
        if (description.isEmpty()) {
            errors.put("description", Messages.format(Messages.MSG02, "Mô tả sự cố"));
        } else if (description.length() > MAX_DESCRIPTION) {
            errors.put("description", Messages.format(Messages.MSG08, MAX_DESCRIPTION));
        }
        return errors;
    }

    // Trả về số vé còn hiệu lực của ghế vừa bị khóa (0 nếu báo cáo không gắn với ghế)
    public int submit(EquipmentReportForm form, int staffId) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        EquipmentReport report = toReport(form, staffId);

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                reportDAO.insert(conn, report);
                if (report.getSeatId() != null) {
                    // Ghế hỏng phải khóa ngay để không bán tiếp (rule "Defective seat operational lock")
                    seatDAO.updateStatus(conn, report.getSeatId(), RoomStatus.MAINTENANCE);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return report.getSeatId() == null ? 0 : seatDAO.countUpcomingTickets(report.getSeatId());
    }

    private EquipmentReport toReport(EquipmentReportForm form, int staffId) {
        EquipmentReport report = new EquipmentReport();
        report.setStaffId(staffId);
        report.setRoomId(parseId(form.getRoomId()));
        report.setSeatId(EquipmentCategory.SEAT.equals(form.getCategory()) ? parseId(form.getSeatId()) : null);
        report.setEquipmentCategory(form.getCategory());
        report.setUrgencyLevel(form.getUrgency());
        report.setDescription(form.getDescription().trim());
        report.setStatus(ReportStatus.PENDING);
        return report;
    }

    private boolean roomExists(int roomId) throws SQLException {
        for (Room room : roomDAO.findAll()) {
            if (room.getRoomId() == roomId) {
                return true;
            }
        }
        return false;
    }

    private int parseId(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
