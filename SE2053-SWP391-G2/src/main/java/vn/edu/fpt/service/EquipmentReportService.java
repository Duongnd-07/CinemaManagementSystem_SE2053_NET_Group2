package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.dao.EquipmentReportDAO;
import vn.edu.fpt.dao.EquipmentReportLogDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.RoomEquipmentDAO;
import vn.edu.fpt.dao.SeatDAO;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.EquipmentReportLog;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomEquipment;
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
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    private static final String LOG_ROOM_AUTO_UNLOCKED =
            "Tự động mở khóa phòng chiếu: không còn sự cố nào chưa xử lý.";
    private static final String LOG_ROOM_AUTO_UNLOCKED_BELOW_LIMIT =
            "Tự động mở khóa phòng chiếu: còn dưới 2 thiết bị (không tính ghế) hỏng mức Cao/Khẩn cấp.";
    private static final String EQUIPMENT_DUPLICATE =
            "Thiết bị này của phòng đã có báo cáo #SC-{0} chưa xử lý xong, không cần báo lại.";
    private static final String UNIT_DUPLICATE = "{0} đã có báo cáo #SC-{1} chưa xử lý xong.";
    private static final String UNIT_INVALID = "Thiết bị không thuộc phòng và loại thiết bị đã chọn.";
    private static final String LOG_ROOM_AUTO_LOCKED =
            "Tự động khóa phòng chiếu: từ 2 thiết bị (không tính ghế) mức Cao/Khẩn cấp chưa xử lý.";
    // Từ AUTO_LOCK_DEVICES thiết bị hỏng (không tính ghế) ở các mức này chưa xử lý xong thì phòng tự khóa bảo trì
    private static final List<String> AUTO_LOCK_URGENCIES = List.of(UrgencyLevel.HIGH, UrgencyLevel.CRITICAL);
    private static final int AUTO_LOCK_DEVICES = 2;
    private static final String ROOM_UNLOCK_BLOCKED = "Không thể mở khóa phòng: phòng còn từ 2 thiết bị (không tính ghế) "
            + "hỏng mức Cao/Khẩn cấp chưa xử lý xong.";

    private final EquipmentReportDAO reportDAO = new EquipmentReportDAO();
    private final EquipmentReportLogDAO logDAO = new EquipmentReportLogDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final RoomEquipmentDAO equipmentDAO = new RoomEquipmentDAO();

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

    // Thiết bị cụ thể của mọi phòng; form lọc theo phòng và loại thiết bị đang chọn
    public List<RoomEquipment> listEquipment() throws SQLException {
        return equipmentDAO.findAll();
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

        boolean placeValid = !errors.containsKey("category") && !errors.containsKey("roomId");
        if (EquipmentCategory.SEAT.equals(form.getCategory())) {
            validateSeats(form, roomId, errors);
        } else if (placeValid && !EquipmentCategory.OTHER.equals(form.getCategory())) {
            validateUnits(form, roomId, errors);
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

    // Mỗi ghế phải thuộc phòng đã chọn và chưa có báo cáo nào đang mở (tránh nhiều nhân viên báo trùng)
    private void validateSeats(EquipmentReportForm form, int roomId, Map<String, String> errors) throws SQLException {
        Set<Integer> seatIds = toIds(form.getSeatIds());
        if (seatIds.isEmpty()) {
            errors.put("seatId", Messages.format(Messages.MSG02, "Ghế"));
            return;
        }
        List<String> duplicates = new ArrayList<>();
        for (int seatId : seatIds) {
            Seat seat = seatDAO.findById(seatId);
            if (seat == null || seat.getRoomId() != roomId) {
                errors.put("seatId", Messages.REPORT_SEAT_INVALID);
                return;
            }
            Integer openReportId = reportDAO.findOpenReportIdBySeat(seatId);
            if (openReportId != null) {
                duplicates.add(Messages.format(Messages.REPORT_SEAT_DUPLICATE,
                        seat.getRowName() + seat.getColNumber(), String.valueOf(openReportId)));
            }
        }
        if (!duplicates.isEmpty()) {
            errors.put("seatId", String.join(" ", duplicates));
        }
    }

    // Phòng đã khai báo thiết bị thì phải chọn thiết bị cụ thể, mỗi thiết bị chỉ một báo cáo đang mở.
    // Phòng chưa khai báo thì giữ cách cũ: mỗi loại thiết bị của phòng chỉ một báo cáo đang mở
    private void validateUnits(EquipmentReportForm form, int roomId, Map<String, String> errors) throws SQLException {
        Map<Integer, RoomEquipment> units = new LinkedHashMap<>();
        for (RoomEquipment unit : equipmentDAO.findByRoomAndCategory(roomId, form.getCategory())) {
            units.put(unit.getEquipmentId(), unit);
        }
        Set<Integer> unitIds = toIds(form.getEquipmentIds());
        if (units.isEmpty()) {
            if (!unitIds.isEmpty()) {
                errors.put("equipmentId", UNIT_INVALID);
                return;
            }
            Integer openReportId = reportDAO.findOpenReportIdByEquipment(roomId, form.getCategory());
            if (openReportId != null) {
                errors.put("category", Messages.format(EQUIPMENT_DUPLICATE, String.valueOf(openReportId)));
            }
            return;
        }
        if (unitIds.isEmpty()) {
            errors.put("equipmentId", Messages.format(Messages.MSG02, "Thiết bị"));
            return;
        }
        List<String> duplicates = new ArrayList<>();
        for (int unitId : unitIds) {
            RoomEquipment unit = units.get(unitId);
            if (unit == null) {
                errors.put("equipmentId", UNIT_INVALID);
                return;
            }
            Integer openReportId = reportDAO.findOpenReportIdByUnit(unitId);
            if (openReportId != null) {
                duplicates.add(Messages.format(UNIT_DUPLICATE, unit.getName(), String.valueOf(openReportId)));
            }
        }
        if (!duplicates.isEmpty()) {
            errors.put("equipmentId", String.join(" ", duplicates));
        }
    }

    // Một lần gửi tạo một báo cáo cho mỗi ghế / thiết bị đã chọn, tất cả trong cùng một transaction
    public SubmitResult submit(EquipmentReportForm form, int staffId) throws SQLException, ValidationException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        boolean isSeat = EquipmentCategory.SEAT.equals(form.getCategory());
        Set<Integer> seatIds = isSeat ? toIds(form.getSeatIds()) : Set.of();
        // Loại "Khác" không có thiết bị cụ thể nên bỏ qua mọi equipmentIds gửi kèm
        boolean hasUnits = !isSeat && !EquipmentCategory.OTHER.equals(form.getCategory());
        Set<Integer> unitIds = hasUnits ? toIds(form.getEquipmentIds()) : Set.of();
        SubmitResult result = new SubmitResult();

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int lastReportId = 0;
                for (int seatId : seatIds) {
                    reportDAO.insert(conn, toReport(form, staffId, seatId), null);
                    // Ghế hỏng phải khóa ngay để không bán tiếp (rule "Defective seat operational lock")
                    seatDAO.updateStatus(conn, seatId, RoomStatus.MAINTENANCE);
                }
                for (int unitId : unitIds) {
                    lastReportId = reportDAO.insert(conn, toReport(form, staffId, null), unitId);
                }
                if (seatIds.isEmpty() && unitIds.isEmpty()) {
                    lastReportId = reportDAO.insert(conn, toReport(form, staffId, null), null);
                }
                // Chỉ xét khi lần gửi này là thiết bị mức Cao/Khẩn cấp, để Manager đã mở khóa thủ công (UC-26)
                // không bị khóa lại cho tới khi có báo cáo khẩn mới; đếm cộng dồn cả báo cáo gửi trước đó
                int roomId = parseId(form.getRoomId());
                if (!isSeat && AUTO_LOCK_URGENCIES.contains(form.getUrgency())
                        && reportDAO.countOpenDevicesByUrgency(conn, roomId, AUTO_LOCK_URGENCIES) >= AUTO_LOCK_DEVICES
                        && roomDAO.lockIfActive(conn, roomId)) {
                    writeLog(conn, lastReportId, staffId, ReportStatus.PENDING, ReportStatus.PENDING,
                            LOG_ROOM_AUTO_LOCKED);
                    result.roomLocked = true;
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                // Hai nhân viên gửi cùng lúc: unique index chặn bản thứ hai, validate lại để báo đúng mã báo cáo trùng
                if (e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                    Map<String, String> duplicate = validate(form);
                    if (!duplicate.isEmpty()) {
                        throw new ValidationException(duplicate);
                    }
                }
                throw e;
            }
        }
        result.reportCount = Math.max(1, seatIds.size() + unitIds.size());
        for (int seatId : seatIds) {
            result.affectedTickets += seatDAO.countUpcomingTickets(seatId);
        }
        if (result.roomLocked) {
            result.upcomingShowtimes = roomDAO.countUpcomingShowtimes(parseId(form.getRoomId()));
        }
        return result;
    }

    // Kết quả gửi báo cáo để Controller dựng thông báo
    public static final class SubmitResult {
        private int reportCount;
        private int affectedTickets;
        private boolean roomLocked;
        private int upcomingShowtimes;

        public int getReportCount() {
            return reportCount;
        }

        // Số vé còn hiệu lực trên các ghế vừa bị khóa
        public int getAffectedTickets() {
            return affectedTickets;
        }

        // Phòng vừa bị tự động khóa bảo trì trong lần gửi này
        public boolean isRoomLocked() {
            return roomLocked;
        }

        // Số suất chiếu sắp tới của phòng vừa bị khóa, cần Manager xử lý
        public int getUpcomingShowtimes() {
            return upcomingShowtimes;
        }
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
                // Phòng tự mở khóa khi sự cố cuối cùng của phòng xử lý xong; còn sự cố dở thì chỉ mở khi quản lý bỏ tích
                boolean resolved = statusChanged && ReportStatus.RESOLVED.equals(newStatus);
                boolean roomClear = resolved && reportDAO.countOtherOpenByRoom(conn, report.getRoomId(), reportId) == 0;
                // Xử lý xong một thiết bị hỏng nặng làm phòng còn dưới ngưỡng tự khóa (UC-21) thì cũng tự mở;
                // ghế hỏng còn lại đã bị khóa riêng từng ghế nên không cần giữ khóa cả phòng
                boolean belowLockLimit = resolved && !roomClear && report.getSeatId() == null
                        && AUTO_LOCK_URGENCIES.contains(report.getUrgencyLevel())
                        && reportDAO.countOpenDevicesByUrgency(conn, report.getRoomId(), AUTO_LOCK_URGENCIES)
                        < AUTO_LOCK_DEVICES;
                boolean autoUnlock = roomClear || belowLockLimit;
                boolean lockRoom = form.isLockRoom() && !autoUnlock;
                // Còn đủ thiết bị hỏng nặng để tự khóa (UC-21) thì không cho bỏ tích; đếm sau khi đã đổi trạng thái ở trên
                if (!lockRoom && !autoUnlock && isRoomLocked(report) && reportDAO.countOpenDevicesByUrgency(
                        conn, report.getRoomId(), AUTO_LOCK_URGENCIES) >= AUTO_LOCK_DEVICES) {
                    throw new ValidationException(Map.of("general", ROOM_UNLOCK_BLOCKED));
                }
                if (lockRoom != isRoomLocked(report)) {
                    roomDAO.updateStatus(conn, report.getRoomId(),
                            lockRoom ? RoomStatus.MAINTENANCE : RoomStatus.ACTIVE);
                    writeLog(conn, reportId, managerId, newStatus, newStatus, lockRoom
                            ? Messages.REPORT_LOG_ROOM_LOCKED
                            : roomClear ? LOG_ROOM_AUTO_UNLOCKED
                            : belowLockLimit ? LOG_ROOM_AUTO_UNLOCKED_BELOW_LIMIT : Messages.REPORT_LOG_ROOM_UNLOCKED);
                    result.roomLocked = lockRoom;
                    result.roomUnlocked = !lockRoom;
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

    private EquipmentReport toReport(EquipmentReportForm form, int staffId, Integer seatId) {
        EquipmentReport report = new EquipmentReport();
        report.setStaffId(staffId);
        report.setRoomId(parseId(form.getRoomId()));
        report.setSeatId(seatId);
        report.setEquipmentCategory(form.getCategory());
        report.setUrgencyLevel(form.getUrgency());
        report.setDescription(form.getDescription().trim());
        report.setStatus(ReportStatus.PENDING);
        return report;
    }

    // Bỏ trùng và giữ thứ tự; giá trị không phải số thành 0 để bị báo là không hợp lệ
    private Set<Integer> toIds(List<String> values) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (String value : values) {
            ids.add(parseId(value));
        }
        return ids;
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
