package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.dao.RoomInspectionDAO;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.InspectionSlot;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-35: Inspect & Clean Screening Room - kiểm tra ghế và dọn phòng trước mỗi suất chiếu.
// Không bao giờ chặn bán vé (BR-35-03); ghế/thiết bị hỏng báo qua UC-21, UC-35 không tự đổi trạng thái ghế (BR-35-05).
// So giờ bằng đồng hồ của web server, cùng giả định với ShiftService (web server và SQL Server cùng giờ).
public class RoomInspectionService {
    public static final String SAVED = "Đã lưu kết quả kiểm tra phòng chiếu.";
    public static final String UPDATED = "Đã cập nhật kết quả kiểm tra phòng chiếu.";
    public static final String UNDONE = "Đã hoàn tác kết quả kiểm tra phòng chiếu.";
    public static final String WINDOW_NOT_OPEN =
            "Chưa đến thời gian kiểm tra suất chiếu này. Thời gian kiểm tra bắt đầu lúc {0}.";
    public static final String REQUIRED_NOT_TICKED =
            "Vui lòng đánh dấu đã kiểm tra ghế và đã dọn dẹp phòng trước khi xác nhận.";
    public static final String ALREADY_INSPECTED = "Suất chiếu này đã được {0} kiểm tra lúc {1}.";
    public static final String EDIT_NOT_ALLOWED =
            "Chỉ {0} mới được sửa kết quả kiểm tra này trong vòng 5 phút sau khi lưu.";
    public static final String NOT_AVAILABLE = "Suất chiếu đã bị hủy hoặc đã kết thúc, không thể kiểm tra.";
    public static final String NOT_INSPECTED_YET = "Suất chiếu này chưa được kiểm tra.";
    public static final String URGENT_BADGE = "{0} phòng chưa được kiểm tra cho các suất chiếu trong 30 phút tới";
    public static final String AFFECTED_TICKETS =
            "Ghế vừa báo hỏng đang có {0} vé cho suất chiếu sắp tới, cần đổi ghế hoặc hoàn tiền khi khách đến quầy.";

    // Suất đầu ngày của phòng: mở kiểm tra trước giờ chiếu 30 phút (BR-35-02)
    public static final int FIRST_SHOWTIME_WINDOW_MINUTES = 30;
    // Hạn sửa / hoàn tác sau khi lưu (BR-35-07)
    public static final int EDIT_MINUTES = 5;
    // Suất chưa kiểm tra bắt đầu trong khoảng này được tô nổi bật và tính vào cảnh báo (BR-35-03)
    public static final int URGENT_MINUTES = 30;
    public static final int MAX_NOTE = 500;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final RoomInspectionDAO inspectionDAO = new RoomInspectionDAO();
    private final ShiftService shiftService = new ShiftService();

    // Các suất chiếu hôm nay (bước 2 của Normal Flow)
    public List<InspectionSlot> listToday(int staffId) throws SQLException {
        return listByDate(LocalDate.now(), 0, staffId);
    }

    // A5: lịch sử theo ngày, phòng (0 = tất cả) và trạng thái ("" = tất cả, "Late" = kiểm tra chậm)
    public List<InspectionSlot> history(LocalDate date, int roomId, String status) throws SQLException {
        List<InspectionSlot> result = new ArrayList<>();
        for (InspectionSlot slot : listByDate(date, roomId, 0)) {
            boolean match = status == null || status.isEmpty()
                    || ("Late".equals(status) ? slot.isLate() : status.equals(slot.getStatus()));
            if (match) {
                result.add(slot);
            }
        }
        return result;
    }

    // Số suất chưa kiểm tra, chưa bắt đầu và bắt đầu trong 30 phút tới
    public int countUrgent(List<InspectionSlot> slots) {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (InspectionSlot slot : slots) {
            if (!slot.isInspected() && slot.getStartTime().isAfter(now)
                    && !slot.getStartTime().isAfter(now.plusMinutes(URGENT_MINUTES))) {
                count++;
            }
        }
        return count;
    }

    public String urgentMessage(int count) {
        return Messages.format(URGENT_BADGE, String.valueOf(count));
    }

    // Một suất chiếu đã tính trạng thái; null nếu không tồn tại
    public InspectionSlot findSlot(int showtimeId, int staffId) throws SQLException {
        InspectionSlot slot = showtimeId > 0 ? inspectionDAO.findSlot(showtimeId) : null;
        if (slot != null) {
            compute(slot, LocalDateTime.now(), staffId);
        }
        return slot;
    }

    // Báo cáo sự cố đã gắn với lần kiểm tra
    public List<EquipmentReport> linkedReports(InspectionSlot slot) throws SQLException {
        return slot.isInspected() ? inspectionDAO.findLinkedReports(slot.getInspectionId()) : new ArrayList<>();
    }

    // Báo cáo nhân viên vừa gửi cho phòng (từ lúc mở khung kiểm tra) sẽ được gắn khi xác nhận / cập nhật
    public List<EquipmentReport> pendingReports(InspectionSlot slot, int staffId) throws SQLException {
        return inspectionDAO.findNewReports(slot.getRoomId(), staffId, slot.getWindowOpen(), slot.getInspectionId());
    }

    // A1: số vé suất sắp tới trên các ghế bị báo hỏng (đã gắn hoặc sắp gắn), để chuyển sang UC-20
    public int affectedTickets(List<EquipmentReport> reports) throws SQLException {
        List<Integer> seatIds = new ArrayList<>();
        for (EquipmentReport report : reports) {
            if (EquipmentCategory.SEAT.equals(report.getEquipmentCategory()) && report.getSeatId() != null) {
                seatIds.add(report.getSeatId());
            }
        }
        return inspectionDAO.countUpcomingTicketsOnSeats(seatIds);
    }

    public String affectedMessage(int tickets) {
        return Messages.format(AFFECTED_TICKETS, String.valueOf(tickets));
    }

    // Bước 5-6: xác nhận kiểm tra. Trả về số báo cáo đã gắn.
    public int save(int showtimeId, int staffId, boolean seatsChecked, boolean roomCleaned, boolean equipmentOk,
                    String note) throws SQLException, ValidationException {
        LocalDateTime now = LocalDateTime.now();
        InspectionSlot slot = requireAvailable(showtimeId, staffId, now);
        if (slot.isInspected()) {
            throw alreadyInspected(slot);
        }
        if (now.isBefore(slot.getWindowOpen())) {
            throw error(Messages.format(WINDOW_NOT_OPEN, slot.getWindowOpen().format(TIME)));
        }
        validateTicks(seatsChecked, roomCleaned, note);
        List<EquipmentReport> reports = inspectionDAO.findNewReports(slot.getRoomId(), staffId, slot.getWindowOpen(), 0);
        slot.setStaffId(staffId);
        slot.setSeatsChecked(true);
        slot.setRoomCleaned(true);
        slot.setEquipmentOk(equipmentOk);
        slot.setHasIssue(!reports.isEmpty());
        slot.setNote(cleanNote(note));
        slot.setInspectedAt(now);
        slot.setLate(now.isAfter(slot.getStartTime()));
        Shift shift = shiftService.findOpenShift(staffId);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int inspectionId = inspectionDAO.insert(conn, slot, shift == null ? null : shift.getShiftId());
                inspectionDAO.linkReports(conn, inspectionId, reports);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == SQL_UNIQUE_VIOLATION || e.getErrorCode() == SQL_UNIQUE_INDEX_VIOLATION) {
                    // E3: nhân viên khác vừa lưu trước
                    throw alreadyInspected(inspectionDAO.findSlot(showtimeId));
                }
                throw e;
            }
        }
        return reports.size();
    }

    // A4: sửa dấu tick / ghi chú trong 5 phút; báo cáo mới gửi thêm cũng được gắn vào
    public int update(int showtimeId, int staffId, boolean seatsChecked, boolean roomCleaned, boolean equipmentOk,
                      String note) throws SQLException, ValidationException {
        LocalDateTime now = LocalDateTime.now();
        InspectionSlot slot = requireEditable(showtimeId, staffId, now);
        validateTicks(seatsChecked, roomCleaned, note);
        List<EquipmentReport> reports = inspectionDAO.findNewReports(slot.getRoomId(), staffId, slot.getWindowOpen(),
                slot.getInspectionId());
        slot.setEquipmentOk(equipmentOk);
        slot.setHasIssue(slot.isHasIssue() || !reports.isEmpty());
        slot.setNote(cleanNote(note));
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!inspectionDAO.update(conn, slot, now.minusMinutes(EDIT_MINUTES))) {
                    conn.rollback();
                    throw error(Messages.format(EDIT_NOT_ALLOWED, nameOf(slot)));
                }
                inspectionDAO.linkReports(conn, slot.getInspectionId(), reports);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return reports.size();
    }

    // A4: hoàn tác trong 5 phút, suất chiếu trở về "Chưa kiểm tra"
    public void undo(int showtimeId, int staffId) throws SQLException, ValidationException {
        LocalDateTime now = LocalDateTime.now();
        InspectionSlot slot = requireEditable(showtimeId, staffId, now);
        if (!inspectionDAO.delete(slot.getInspectionId(), staffId, now.minusMinutes(EDIT_MINUTES))) {
            throw error(Messages.format(EDIT_NOT_ALLOWED, nameOf(slot)));
        }
    }

    private List<InspectionSlot> listByDate(LocalDate date, int roomId, int staffId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        List<InspectionSlot> slots = inspectionDAO.findSlots(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), roomId);
        for (InspectionSlot slot : slots) {
            compute(slot, now, staffId);
        }
        return slots;
    }

    // BR-35-02: khung kiểm tra mở khi suất trước cùng phòng kết thúc, hoặc 30 phút trước suất đầu ngày
    private void compute(InspectionSlot slot, LocalDateTime now, int staffId) {
        LocalDateTime windowOpen = slot.getPreviousEnd() != null ? slot.getPreviousEnd()
                : slot.getStartTime().minusMinutes(FIRST_SHOWTIME_WINDOW_MINUTES);
        slot.setWindowOpen(windowOpen);
        boolean unavailable = isUnavailable(slot, now);
        if (slot.isInspected()) {
            slot.setStatus(slot.isHasIssue() ? InspectionSlot.HAS_ISSUE : InspectionSlot.OK);
        } else {
            slot.setStatus(unavailable ? InspectionSlot.MISSED : InspectionSlot.NOT_INSPECTED);
        }
        slot.setWindowOpenNow(!unavailable && !now.isBefore(windowOpen));
        slot.setUrgent(!slot.isInspected() && !unavailable
                && !slot.getStartTime().isAfter(now.plusMinutes(URGENT_MINUTES)));
        slot.setEditable(slot.isInspected() && !unavailable && staffId > 0 && slot.getStaffId() == staffId
                && !slot.getInspectedAt().isBefore(now.minusMinutes(EDIT_MINUTES)));
    }

    // Suất đã hủy hoặc đã kết thúc (E5)
    private boolean isUnavailable(InspectionSlot slot, LocalDateTime now) {
        return ShowtimeStatus.CANCELLED.equals(slot.getShowtimeStatus())
                || ShowtimeStatus.ENDED.equals(slot.getShowtimeStatus())
                || !now.isBefore(slot.getEndTime());
    }

    private InspectionSlot requireAvailable(int showtimeId, int staffId, LocalDateTime now)
            throws SQLException, ValidationException {
        InspectionSlot slot = showtimeId > 0 ? inspectionDAO.findSlot(showtimeId) : null;
        if (slot == null) {
            throw error(NOT_AVAILABLE);
        }
        compute(slot, now, staffId);
        if (isUnavailable(slot, now)) {
            throw error(NOT_AVAILABLE);
        }
        return slot;
    }

    // E4: chỉ người đã lưu, trong 5 phút sau khi lưu
    private InspectionSlot requireEditable(int showtimeId, int staffId, LocalDateTime now)
            throws SQLException, ValidationException {
        InspectionSlot slot = requireAvailable(showtimeId, staffId, now);
        if (!slot.isInspected()) {
            throw error(NOT_INSPECTED_YET);
        }
        if (!slot.isEditable()) {
            throw error(Messages.format(EDIT_NOT_ALLOWED, nameOf(slot)));
        }
        return slot;
    }

    // E2 + độ dài ghi chú
    private void validateTicks(boolean seatsChecked, boolean roomCleaned, String note) throws ValidationException {
        if (!seatsChecked || !roomCleaned) {
            throw error(REQUIRED_NOT_TICKED);
        }
        if (note != null && note.trim().length() > MAX_NOTE) {
            throw new ValidationException(Map.of("note", Messages.format(Messages.MSG08, String.valueOf(MAX_NOTE))));
        }
    }

    private ValidationException alreadyInspected(InspectionSlot slot) {
        if (slot == null || slot.getInspectedAt() == null) {
            return error(NOT_AVAILABLE);
        }
        return error(Messages.format(ALREADY_INSPECTED, nameOf(slot), slot.getInspectedAt().format(TIME)));
    }

    private String nameOf(InspectionSlot slot) {
        return slot.getStaffName() == null ? "nhân viên khác" : slot.getStaffName();
    }

    private String cleanNote(String note) {
        String value = note == null ? "" : note.trim();
        return value.isEmpty() ? null : value;
    }

    private ValidationException error(String message) {
        return new ValidationException(Map.of("general", message));
    }

    // Dùng cho bộ lọc trạng thái ở lịch sử
    public static final Set<String> HISTORY_STATUSES = Set.of(InspectionSlot.NOT_INSPECTED, InspectionSlot.OK,
            InspectionSlot.HAS_ISSUE, InspectionSlot.MISSED, "Late");
}
