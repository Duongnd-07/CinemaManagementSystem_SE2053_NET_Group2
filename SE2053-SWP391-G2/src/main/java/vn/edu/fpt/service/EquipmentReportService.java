package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.EquipmentReportDAO;
import vn.edu.fpt.dao.EquipmentReportLogDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.SeatDAO;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.EquipmentReportLog;
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
    private static final int MAX_NOTE = 500;

    private final EquipmentReportDAO reportDAO = new EquipmentReportDAO();
    private final EquipmentReportLogDAO logDAO = new EquipmentReportLogDAO();
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

    // Sự cố chưa xử lý xong của mọi nhân viên, form hiển thị theo phòng để tránh báo trùng
    public List<EquipmentReport> listOpenReports() throws SQLException {
        return reportDAO.findOpen();
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
                } else {
                    // Một ghế chỉ có một báo cáo đang mở, tránh nhiều nhân viên báo trùng
                    Integer openReportId = reportDAO.findOpenReportIdBySeat(seat.getSeatId());
                    if (openReportId != null) {
                        errors.put("seatId", Messages.format(Messages.REPORT_SEAT_DUPLICATE,
                                seat.getRowName() + seat.getColNumber(), String.valueOf(openReportId)));
                    }
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

    // ----- UC-26: quản lý xử lý báo cáo -----

    public Page<EquipmentReport> listAll(String status, int page) throws SQLException {
        String statusFilter = ReportStatus.isValid(status) ? status : null;
        return Page.of(page, PAGE_SIZE, reportDAO.countAll(statusFilter),
                (offset, limit) -> reportDAO.searchAll(statusFilter, offset, limit));
    }

    public Map<String, Integer> countAllByStatus() throws SQLException {
        return reportDAO.countAllByStatus();
    }

    public EquipmentReport findById(int reportId) throws SQLException {
        return reportDAO.findById(reportId);
    }

    public List<EquipmentReportLog> listLogs(int reportId) throws SQLException {
        return logDAO.findByReport(reportId);
    }

    // Vòng đời chỉ đi tới: từ trạng thái hiện tại trở về sau
    public List<String> allowedStatuses(String currentStatus) {
        int from = Math.max(0, ReportStatus.ALL.indexOf(currentStatus));
        return ReportStatus.ALL.subList(from, ReportStatus.ALL.size());
    }

    public Map<String, String> validateUpdate(EquipmentReport report, EquipmentReportUpdateForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String newStatus = resolveNewStatus(report, form);
        if (!ReportStatus.isValid(newStatus)) {
            errors.put("newStatus", Messages.REPORT_STATUS_INVALID);
        } else if (!allowedStatuses(report.getStatus()).contains(newStatus)) {
            errors.put("newStatus", Messages.REPORT_STATUS_BACKWARD);
        }
        if (form.getNote().trim().length() > MAX_NOTE) {
            errors.put("note", Messages.format(Messages.MSG08, MAX_NOTE));
        }
        if (errors.isEmpty() && newStatus.equals(report.getStatus()) && form.getNote().trim().isEmpty()
                && form.isLockRoom() == isRoomLocked(report)) {
            errors.put("general", Messages.REPORT_NO_CHANGE);
        }
        return errors;
    }

    // Đổi trạng thái, ghi chú tiến độ, khóa/mở phòng; mọi thao tác đều ghi EquipmentReportLog
    public UpdateResult process(EquipmentReport report, EquipmentReportUpdateForm form, int managerId)
            throws SQLException, ValidationException {
        Map<String, String> errors = validateUpdate(report, form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        int reportId = report.getReportId();
        String oldStatus = report.getStatus();
        String newStatus = resolveNewStatus(report, form);
        String note = form.getNote().trim();
        boolean statusChanged = !newStatus.equals(oldStatus);
        boolean lockChanged = form.isLockRoom() != isRoomLocked(report);
        UpdateResult result = new UpdateResult();

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (statusChanged && !reportDAO.updateStatus(conn, reportId, oldStatus, newStatus)) {
                    throw new ValidationException(Map.of("general", Messages.REPORT_CHANGED));
                }
                if (statusChanged || !note.isEmpty()) {
                    writeLog(conn, reportId, managerId, oldStatus, newStatus, note.isEmpty() ? null : note);
                }
                if (lockChanged) {
                    roomDAO.updateStatus(conn, report.getRoomId(),
                            form.isLockRoom() ? RoomStatus.MAINTENANCE : RoomStatus.ACTIVE);
                    writeLog(conn, reportId, managerId, newStatus, newStatus, form.isLockRoom()
                            ? Messages.REPORT_LOG_ROOM_LOCKED : Messages.REPORT_LOG_ROOM_UNLOCKED);
                    result.roomLocked = form.isLockRoom();
                    result.roomUnlocked = !form.isLockRoom();
                }
                // Ghế chỉ mở lại khi không còn báo cáo nào khác của ghế đó đang dở
                if (statusChanged && ReportStatus.RESOLVED.equals(newStatus) && report.getSeatId() != null
                        && reportDAO.countOtherOpenBySeat(conn, report.getSeatId(), reportId) == 0) {
                    seatDAO.updateStatus(conn, report.getSeatId(), RoomStatus.ACTIVE);
                    writeLog(conn, reportId, managerId, newStatus, newStatus,
                            Messages.format(Messages.REPORT_LOG_SEAT_REOPENED, report.getSeatLabel()));
                    result.seatReopened = true;
                }
                conn.commit();
            } catch (SQLException | ValidationException e) {
                conn.rollback();
                throw e;
            }
        }
        if (result.roomLocked) {
            result.upcomingShowtimes = roomDAO.countUpcomingShowtimes(report.getRoomId());
        }
        return result;
    }

    public boolean isRoomLocked(EquipmentReport report) {
        return RoomStatus.MAINTENANCE.equals(report.getRoomStatus());
    }

    // Form của báo cáo đã xử lý xong không gửi trạng thái, khi đó giữ nguyên
    private String resolveNewStatus(EquipmentReport report, EquipmentReportUpdateForm form) {
        return form.getNewStatus().isEmpty() ? report.getStatus() : form.getNewStatus();
    }

    private void writeLog(Connection conn, int reportId, int managerId, String oldStatus, String newStatus,
                          String note) throws SQLException {
        EquipmentReportLog log = new EquipmentReportLog();
        log.setReportId(reportId);
        log.setManagerId(managerId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setNote(note);
        logDAO.insert(conn, log);
    }

    // Kết quả xử lý để Controller dựng thông báo
    public static final class UpdateResult {
        private boolean roomLocked;
        private boolean roomUnlocked;
        private boolean seatReopened;
        private int upcomingShowtimes;

        public boolean isRoomLocked() {
            return roomLocked;
        }

        public boolean isRoomUnlocked() {
            return roomUnlocked;
        }

        public boolean isSeatReopened() {
            return seatReopened;
        }

        public int getUpcomingShowtimes() {
            return upcomingShowtimes;
        }
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
