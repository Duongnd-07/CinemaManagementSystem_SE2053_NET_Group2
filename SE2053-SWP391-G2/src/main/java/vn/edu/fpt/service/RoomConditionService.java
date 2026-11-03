package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.RoomConditionDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.ShiftDAO;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomCheckItem;
import vn.edu.fpt.model.RoomConditionItem;
import vn.edu.fpt.model.RoomConditionReport;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

// UC-35: Report Room Condition Before Shift
public class RoomConditionService {
    public static final String ERROR_GENERAL = "general";
    public static final String ERROR_ITEM_PREFIX = "item_";
    private static final int PAGE_SIZE = 10;
    private static final int MAX_NOTE = 500;
    private static final int MAX_ITEM_NOTE = 200;

    private final RoomConditionDAO conditionDAO = new RoomConditionDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final ShiftDAO shiftDAO = new ShiftDAO();

    // Phòng dừng hoạt động (UC-25) không cần kiểm tra
    public List<Room> listRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        for (Room room : roomDAO.findAll()) {
            if (!RoomStatus.INACTIVE.equals(room.getStatus())) {
                rooms.add(room);
            }
        }
        return rooms;
    }

    public Map<Integer, RoomConditionReport> latestToday() throws SQLException {
        return conditionDAO.findLatestByRoom(LocalDate.now());
    }

    public int countUncheckedToday() throws SQLException {
        Map<Integer, RoomConditionReport> latest = latestToday();
        int unchecked = 0;
        for (Room room : listRooms()) {
            if (!latest.containsKey(room.getRoomId())) {
                unchecked++;
            }
        }
        return unchecked;
    }

    public boolean isInShift(int staffId) throws SQLException {
        return shiftDAO.findOpenByStaff(staffId) != null;
    }

    public Page<RoomConditionReport> listByStaff(int staffId, int page) throws SQLException {
        return Page.of(page, PAGE_SIZE, conditionDAO.count(staffId, null, null),
                (offset, limit) -> conditionDAO.search(staffId, null, null, offset, limit));
    }

    public Page<RoomConditionReport> listByDate(LocalDate date, String result, int page) throws SQLException {
        String filter = RoomConditionReport.GOOD.equals(result) || RoomConditionReport.ISSUE.equals(result)
                ? result : null;
        return Page.of(page, PAGE_SIZE, conditionDAO.count(null, date, filter),
                (offset, limit) -> conditionDAO.search(null, date, filter, offset, limit));
    }

    // Trả về báo cáo vừa lưu; chỉ nhận khi nhân viên chưa mở ca và chưa báo phòng này cho ca sắp tới
    public RoomConditionReport submit(RoomConditionForm form, int staffId) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        RoomConditionReport report = new RoomConditionReport();
        report.setStaffId(staffId);
        validate(form, report, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (isInShift(staffId)) {
            throw new ValidationException(Map.of(ERROR_GENERAL, Messages.ROOM_CONDITION_IN_SHIFT));
        }
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (conditionDAO.existsForUpcomingShift(conn, staffId, report.getRoomId())) {
                    conn.rollback();
                    throw new ValidationException(Map.of(ERROR_GENERAL, Messages.ROOM_CONDITION_DUPLICATE));
                }
                report.setReportId(conditionDAO.insert(conn, report));
                conn.commit();
                return report;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private void validate(RoomConditionForm form, RoomConditionReport report, Map<String, String> errors)
            throws SQLException {
        Room room = findCheckableRoom(form.getRoomId());
        if (room == null) {
            errors.put("roomId", form.getRoomId().isEmpty()
                    ? Messages.format(Messages.MSG02, "Phòng chiếu") : Messages.REPORT_ROOM_INVALID);
        } else {
            report.setRoomId(room.getRoomId());
            report.setRoomName(room.getRoomName());
        }

        boolean anyFailed = false;
        for (RoomCheckItem item : RoomCheckItem.values()) {
            String result = form.getResults().getOrDefault(item.getKey(), "");
            String note = form.getItemNotes().getOrDefault(item.getKey(), "").trim();
            String errorKey = ERROR_ITEM_PREFIX + item.getKey();
            if (RoomConditionForm.OK.equals(result)) {
                report.getItems().add(new RoomConditionItem(item.getKey(), true, null));
            } else if (!RoomConditionForm.FAIL.equals(result)) {
                errors.put(errorKey, Messages.format(Messages.MSG02, item.getLabel()));
            } else if (note.isEmpty()) {
                errors.put(errorKey, Messages.format(Messages.ROOM_CONDITION_NOTE_REQUIRED, item.getLabel()));
            } else if (note.length() > MAX_ITEM_NOTE) {
                errors.put(errorKey, Messages.format(Messages.MSG08, MAX_ITEM_NOTE));
            } else {
                anyFailed = true;
                report.getItems().add(new RoomConditionItem(item.getKey(), false, note));
            }
        }
        report.setResult(anyFailed ? RoomConditionReport.ISSUE : RoomConditionReport.GOOD);

        String note = form.getNote().trim();
        if (note.length() > MAX_NOTE) {
            errors.put("note", Messages.format(Messages.MSG08, MAX_NOTE));
        }
        report.setNote(note.isEmpty() ? null : note);
    }

    private Room findCheckableRoom(String roomId) throws SQLException {
        for (Room room : listRooms()) {
            if (String.valueOf(room.getRoomId()).equals(roomId)) {
                return room;
            }
        }
        return null;
    }
}
