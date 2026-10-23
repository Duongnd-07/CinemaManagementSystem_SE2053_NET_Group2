package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.dao.ShiftDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-22: View Shift Report và vòng đời ca bán tại quầy (mở ca ở UC-19).
// Ca kết thúc theo ba cách: nhân viên tự kết (kèm đếm két), Manager kết hộ, hoặc hệ thống tự kết khi quá
// SHIFT_HOURS giờ. Khi kết, số liệu được chốt vào bảng Shift (BR-03) và không tính lại nữa.
public class ShiftService {
    public static final String ERROR_GENERAL = "general";
    public static final String ERROR_COUNTER = "counterNo";
    public static final String ERROR_OPENING = "openingCash";
    public static final String ERROR_COUNTED = "countedCash";
    public static final String ERROR_NOTE = "cashNote";

    // Mã quầy lưu trong Shift.CounterNo (VARCHAR, không dấu); JSP hiển thị "Quầy 1"...
    public static final List<String> COUNTERS = List.of("Q1", "Q2", "Q3");
    // Trang bán vé báo trước khi ca sắp tự kết để nhân viên hoàn tất đơn đang bán
    public static final int WARNING_MINUTES = 15;

    private static final String CONFIG_SHIFT_HOURS = "SHIFT_HOURS";
    private static final int DEFAULT_SHIFT_HOURS = 4;
    private static final int MAX_NOTE = 500;
    private static final Set<Integer> DUPLICATE_KEY_ERRORS = Set.of(2601, 2627);

    private static final String COUNTER_INVALID = "Vui lòng chọn quầy làm việc.";
    private static final String SHIFT_ALREADY_OPEN = "Bạn đang có một ca chưa kết thúc.";
    private static final String SHIFT_NOT_OPEN = "Bạn không có ca nào đang mở.";
    private static final String SHIFT_NOT_FOUND = "Không tìm thấy ca làm việc.";
    private static final String SHIFT_ALREADY_CLOSED = "Ca này đã kết thúc.";
    private static final String SHIFT_NOT_CLOSED = "Ca chưa kết thúc nên chưa đếm két được.";
    private static final String ALREADY_COUNTED = "Ca này đã được đếm két, không sửa được.";
    private static final String NOT_YOUR_SHIFT = "Bạn chỉ đếm két được cho ca của mình.";
    private static final String MONEY_INVALID = "Số tiền không hợp lệ.";
    private static final String NOTE_REQUIRED = "Tiền đếm được lệch {0}đ so với máy, vui lòng ghi lý do.";

    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final SystemConfigDAO configDAO = new SystemConfigDAO();

    public int shiftHours() throws SQLException {
        return Math.max(1, configDAO.getInt(CONFIG_SHIFT_HOURS, DEFAULT_SHIFT_HOURS));
    }

    public LocalDateTime plannedEnd(Shift shift, int hours) {
        return shift.getStartedAt().plusHours(hours);
    }

    // shortcut: so giờ hết ca bằng đồng hồ của web server, giả định cùng máy/giờ với SQL Server như cấu hình hiện tại;
    // đổi sang so bằng GETDATE() nếu tách DB sang máy khác
    public boolean isExpired(Shift shift, int hours) {
        return !plannedEnd(shift, hours).isAfter(LocalDateTime.now());
    }

    // Ca đang mở của nhân viên; ca quá giờ được kết luôn nên không trả về
    public Shift findOpenShift(int staffId) throws SQLException {
        closeExpired();
        return shiftDAO.findOpenByStaff(staffId);
    }

    public void open(int staffId, String counterNo, String openingCashRaw) throws SQLException, ValidationException {
        if (!COUNTERS.contains(counterNo)) {
            throw error(ERROR_COUNTER, COUNTER_INVALID);
        }
        BigDecimal openingCash = parseMoney(openingCashRaw);
        if (openingCash == null) {
            throw error(ERROR_OPENING, MONEY_INVALID);
        }
        if (findOpenShift(staffId) != null) {
            throw error(ERROR_GENERAL, SHIFT_ALREADY_OPEN);
        }
        try {
            shiftDAO.open(staffId, counterNo, openingCash);
        } catch (SQLException e) {
            // Hai tab cùng bấm Mở ca: UX_Shift_OpenPerStaff chặn bản thứ hai
            if (isDuplicateKey(e)) {
                throw error(ERROR_GENERAL, SHIFT_ALREADY_OPEN);
            }
            throw e;
        }
    }

    // Nhân viên tự kết ca và đếm két ngay; trả về ShiftID để chuyển sang báo cáo của ca vừa kết
    public int closeByStaff(int staffId, String countedRaw, String note) throws SQLException, ValidationException {
        BigDecimal counted = parseMoney(countedRaw);
        if (counted == null) {
            throw error(ERROR_COUNTED, MONEY_INVALID);
        }
        String cleanNote = cleanNote(note);
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Khóa ca để đơn đang bán (UC-19 cũng khóa dòng này) chốt xong rồi mới tính tổng
                Shift shift = shiftDAO.findOpenByStaff(conn, staffId, true);
                if (shift == null) {
                    throw error(ERROR_GENERAL, SHIFT_NOT_OPEN);
                }
                ShiftReport totals = new ShiftReport(shift, null);
                shiftDAO.summarize(conn, shift.getShiftId(), totals);
                shift.setCountedCash(counted);
                requireNoteIfMismatch(totals, cleanNote);
                shiftDAO.close(conn, shift.getShiftId(), null, staffId, totals, counted, cleanNote);
                conn.commit();
                return shift.getShiftId();
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Manager kết hộ ca nhân viên quên kết; két chưa đếm, nhân viên hoặc Manager nhập sau (recordCount)
    public void closeByManager(int shiftId, int managerId) throws SQLException, ValidationException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Shift shift = shiftDAO.findById(conn, shiftId, true);
                if (shift == null) {
                    throw error(ERROR_GENERAL, SHIFT_NOT_FOUND);
                }
                if (shift.getEndedAt() != null) {
                    throw error(ERROR_GENERAL, SHIFT_ALREADY_CLOSED);
                }
                ShiftReport totals = new ShiftReport(shift, null);
                shiftDAO.summarize(conn, shiftId, totals);
                shiftDAO.close(conn, shiftId, null, managerId, totals, null, null);
                conn.commit();
            } catch (SQLException | ValidationException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // Kết mọi ca đã quá SHIFT_HOURS giờ; giờ kết = giờ mở + SHIFT_HOURS. Gọi định kỳ (ShiftAutoCloseListener)
    // và trước khi đọc ca đang mở để không bao giờ thấy ca quá giờ còn mở
    public int closeExpired() throws SQLException {
        int hours = shiftHours();
        int closed = 0;
        try (Connection conn = DBContext.getConnection()) {
            for (Integer shiftId : shiftDAO.findExpiredOpenIds(conn, hours)) {
                conn.setAutoCommit(false);
                try {
                    Shift shift = shiftDAO.findById(conn, shiftId, true);
                    if (shift != null && shift.getEndedAt() == null) {
                        ShiftReport totals = new ShiftReport(shift, null);
                        shiftDAO.summarize(conn, shiftId, totals);
                        if (shiftDAO.close(conn, shiftId, hours, null, totals, null, null)) {
                            closed++;
                        }
                    }
                    conn.commit();
                } catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }
        return closed;
    }

    // Nhập số đếm két cho ca đã kết mà chưa đếm; nhân viên chỉ cho ca của mình, Manager cho mọi ca; chỉ một lần
    public void recordCount(int shiftId, int userId, boolean manager, String countedRaw, String note)
            throws SQLException, ValidationException {
        BigDecimal counted = parseMoney(countedRaw);
        if (counted == null) {
            throw error(ERROR_COUNTED, MONEY_INVALID);
        }
        String cleanNote = cleanNote(note);
        ShiftReport report = report(shiftId);
        if (report == null) {
            throw error(ERROR_GENERAL, SHIFT_NOT_FOUND);
        }
        Shift shift = report.getShift();
        if (!manager && shift.getStaffId() != userId) {
            throw error(ERROR_GENERAL, NOT_YOUR_SHIFT);
        }
        if (!report.isClosed()) {
            throw error(ERROR_GENERAL, SHIFT_NOT_CLOSED);
        }
        if (report.isCounted()) {
            throw error(ERROR_GENERAL, ALREADY_COUNTED);
        }
        shift.setCountedCash(counted);
        requireNoteIfMismatch(report, cleanNote);
        if (!shiftDAO.recordCount(shiftId, counted, cleanNote)) {
            throw error(ERROR_GENERAL, ALREADY_COUNTED);
        }
    }

    // Báo cáo một ca; null nếu không có. Ca đang mở: tính trực tiếp; ca đã kết: dùng số đã chốt
    public ShiftReport report(int shiftId) throws SQLException {
        int hours = shiftHours();
        try (Connection conn = DBContext.getConnection()) {
            Shift shift = shiftDAO.findById(conn, shiftId, false);
            if (shift == null) {
                return null;
            }
            ShiftReport report = new ShiftReport(shift, plannedEnd(shift, hours));
            // Số đơn, số món, giảm giá không có cột chốt riêng; đơn tại quầy đã Paid không đổi nên đọc lại vẫn đúng
            shiftDAO.summarize(conn, shiftId, report);
            if (shift.getClosedAt() != null) {
                report.setTicketCount(shift.getTicketCount());
                report.setTicketRevenue(shift.getTicketRevenue());
                report.setConcessionRevenue(shift.getConcessionRevenue());
                report.setCashTotal(shift.getCashTotal());
                report.setCardTotal(shift.getCardTotal());
                report.setTransferTotal(shift.getTransferTotal());
                report.setRefundTotal(shift.getRefundTotal());
            }
            shiftDAO.findOrders(conn, shiftId, report);
            return report;
        }
    }

    public List<Shift> listMine(int staffId) throws SQLException {
        closeExpired();
        return shiftDAO.findRecentByStaff(staffId);
    }

    public List<Shift> listByDate(LocalDate date) throws SQLException {
        closeExpired();
        return shiftDAO.findByPeriod(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // Thừa hoặc thiếu tiền trong két thì bắt buộc ghi lý do để Manager đối chiếu khi bàn giao
    private void requireNoteIfMismatch(ShiftReport report, String note) throws ValidationException {
        BigDecimal difference = report.getCashDifference();
        if (difference != null && difference.signum() != 0 && note == null) {
            throw error(ERROR_NOTE, Messages.format(NOTE_REQUIRED,
                    String.format("%,d", difference.abs().longValue()).replace(',', '.')));
        }
    }

    private String cleanNote(String note) throws ValidationException {
        String value = note == null ? "" : note.trim();
        if (value.length() > MAX_NOTE) {
            throw error(ERROR_NOTE, Messages.format(Messages.MSG08, MAX_NOTE));
        }
        return value.isEmpty() ? null : value;
    }

    // Số tiền VND không âm, cho phép gõ dấu chấm/phẩy ngăn cách nghìn; null nếu sai
    private BigDecimal parseMoney(String raw) {
        String value = raw == null ? "" : raw.trim().replace(".", "").replace(",", "").replace(" ", "");
        return value.matches("\\d{1,12}") ? new BigDecimal(value) : null;
    }

    private boolean isDuplicateKey(SQLException e) {
        for (SQLException next = e; next != null; next = next.getNextException()) {
            if (DUPLICATE_KEY_ERRORS.contains(next.getErrorCode())) {
                return true;
            }
        }
        return false;
    }

    private ValidationException error(String key, String message) {
        return new ValidationException(Map.of(key, message));
    }
}
