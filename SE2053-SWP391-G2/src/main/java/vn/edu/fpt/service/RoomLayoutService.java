package vn.edu.fpt.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.dao.RoomLayoutDAO;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.RoomSummary;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatType;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;

// UC-25: Manage Rooms & Seat Layout
public class RoomLayoutService {
    public static final int MAX_ROWS = 26;
    public static final int MAX_COLS = 30;
    public static final int MAX_NAME = 50;
    public static final int DEFAULT_ROWS = 8;
    public static final int DEFAULT_COLS = 12;
    // Mã ô trong chuỗi layout (xem RoomLayoutForm)
    public static final char AISLE = '.';
    // Nửa bên phải của ghế đôi; nửa trái là C (dùng được) hoặc c (bị khóa)
    public static final char COUPLE_RIGHT = '=';
    private static final String ROW_SEPARATOR = "/";
    private static final Map<Character, String> TYPE_BY_CODE = Map.of(
            'S', SeatType.STANDARD, 'V', SeatType.VIP, 'C', SeatType.SWEETBOX);
    private static final Map<String, Character> CODE_BY_TYPE = Map.of(
            SeatType.STANDARD, 'S', SeatType.VIP, 'V', SeatType.SWEETBOX, 'C');
    // Trạng thái Manager được chọn; Bảo trì (Maintenance) chỉ do UC-26 bật/tắt
    public static final List<String> MANAGER_STATUSES = List.of(RoomStatus.ACTIVE, RoomStatus.INACTIVE);
    // Số vị trí ghế tối đa liệt kê trong một thông báo lỗi
    private static final int MAX_LISTED = 12;
    private static final int SQL_UNIQUE_VIOLATION = 2627;
    private static final int SQL_UNIQUE_INDEX_VIOLATION = 2601;
    private static final int SQL_FOREIGN_KEY_VIOLATION = 547;

    // Lý do một ghế bị giới hạn thao tác, dùng để gợi ý trên trình chỉnh sửa
    public static final String LOCK_REPORT = "report";
    public static final String LOCK_SOLD = "sold";
    public static final String LOCK_HISTORY = "history";

    private final RoomLayoutDAO roomDAO = new RoomLayoutDAO();

    public List<RoomSummary> listRooms() throws SQLException {
        return roomDAO.findAllSummaries();
    }

    public Room findRoom(int roomId) throws SQLException {
        return roomId > 0 ? roomDAO.findRoom(roomId) : null;
    }

    public int countUpcomingShowtimes(int roomId) throws SQLException {
        return roomId > 0 ? roomDAO.countUpcomingShowtimes(roomId) : 0;
    }

    // Phòng mới: lưới mặc định toàn ghế thường
    public RoomLayoutForm newForm() {
        RoomLayoutForm form = new RoomLayoutForm();
        form.setFormat(MovieService.FORMATS.get(0));
        form.setStatus(RoomStatus.ACTIVE);
        form.setRows(String.valueOf(DEFAULT_ROWS));
        form.setCols(String.valueOf(DEFAULT_COLS));
        form.setLayout(filledLayout(DEFAULT_ROWS, DEFAULT_COLS, 'S'));
        return form;
    }

    // Form Sửa: chỉ tên, định dạng, trạng thái (không dựng lưới ghế)
    public RoomLayoutForm loadInfoForm(int roomId) throws SQLException, ValidationException {
        Room room = roomId > 0 ? roomDAO.findRoom(roomId) : null;
        if (room == null) {
            throw new ValidationException(Map.of("general", Messages.ROOM_NOT_FOUND));
        }
        RoomLayoutForm form = new RoomLayoutForm();
        form.setRoomId(room.getRoomId());
        form.setName(room.getRoomName());
        form.setFormat(room.getFormat() == null ? "" : room.getFormat());
        form.setStatus(room.getStatus() == null ? "" : room.getStatus());
        return form;
    }

    // Phòng có sẵn: dựng lưới từ các ghế đã lưu; phòng chưa có ghế dùng lưới mặc định
    public RoomLayoutForm loadForm(int roomId) throws SQLException, ValidationException {
        Room room = roomId > 0 ? roomDAO.findRoom(roomId) : null;
        if (room == null) {
            throw new ValidationException(Map.of("general", Messages.ROOM_NOT_FOUND));
        }
        RoomLayoutForm form = newForm();
        form.setRoomId(room.getRoomId());
        form.setName(room.getRoomName());
        form.setFormat(room.getFormat() == null ? "" : room.getFormat());
        form.setStatus(room.getStatus() == null ? "" : room.getStatus());
        List<Seat> seats = roomDAO.findSeats(roomId);
        if (seats.isEmpty()) {
            return form;
        }
        int rows = 0;
        int cols = 0;
        for (Seat seat : seats) {
            int row = rowIndex(seat.getRowName());
            int lastCol = seat.getColNumber() + SeatType.widthOf(seat.getSeatType()) - 1;
            if (row < 0 || seat.getColNumber() < 1 || lastCol > MAX_COLS || !SeatType.isValid(seat.getSeatType())) {
                throw unsupportedLayout();
            }
            rows = Math.max(rows, row + 1);
            cols = Math.max(cols, lastCol);
        }
        char[][] grid = new char[rows][cols];
        for (char[] line : grid) {
            Arrays.fill(line, AISLE);
        }
        for (Seat seat : seats) {
            char code = codeOf(seat.getSeatType());
            char[] line = grid[rowIndex(seat.getRowName())];
            int col = seat.getColNumber() - 1;
            // Hai ghế cùng chiếm một ô (dữ liệu ghế đôi kiểu cũ 1 ô) thì không dựng được lưới
            if (line[col] != AISLE || (SeatType.widthOf(seat.getSeatType()) == 2 && line[col + 1] != AISLE)) {
                throw unsupportedLayout();
            }
            line[col] = RoomStatus.ACTIVE.equals(seat.getStatus()) ? code : Character.toLowerCase(code);
            if (SeatType.widthOf(seat.getSeatType()) == 2) {
                line[col + 1] = COUPLE_RIGHT;
            }
        }
        List<String> lines = new ArrayList<>();
        for (char[] line : grid) {
            lines.add(new String(line));
        }
        form.setRows(String.valueOf(rows));
        form.setCols(String.valueOf(cols));
        form.setLayout(String.join(ROW_SEPARATOR, lines));
        return form;
    }

    // Vị trí ghế (vd "A5") -> lý do bị giới hạn: báo cáo sự cố đang mở > đã bán vé sắp chiếu > đã có lịch sử
    public Map<String, String> findLocks(int roomId) throws SQLException {
        Map<String, String> locks = new LinkedHashMap<>();
        if (roomId <= 0) {
            return locks;
        }
        Set<Integer> history = roomDAO.findSeatIdsWithHistory(roomId);
        Set<Integer> sold = roomDAO.findSeatIdsWithUpcomingTickets(roomId);
        Set<Integer> reports = roomDAO.findSeatIdsWithOpenReports(roomId);
        for (Seat seat : roomDAO.findSeats(roomId)) {
            String lock = reports.contains(seat.getSeatId()) ? LOCK_REPORT
                    : sold.contains(seat.getSeatId()) ? LOCK_SOLD
                    : history.contains(seat.getSeatId()) ? LOCK_HISTORY : null;
            if (lock != null) {
                locks.put(seat.getRowName() + seat.getColNumber(), lock);
            }
        }
        return locks;
    }

    // Lưu phòng và toàn bộ sơ đồ ghế trong một giao dịch; trả về RoomID
    public int save(RoomLayoutForm form) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();
        Room current = null;
        if (form.getRoomId() > 0) {
            current = roomDAO.findRoom(form.getRoomId());
            if (current == null) {
                throw new ValidationException(Map.of("general", Messages.ROOM_NOT_FOUND));
            }
        }
        validateInfo(form, current, errors);
        // Phòng mới chọn Hoạt động / Dừng hoạt động; phòng có sẵn đổi trạng thái ở form Sửa (saveInfo)
        String status = form.getStatus() == null ? "" : form.getStatus().trim();
        if (current == null && !MANAGER_STATUSES.contains(status)) {
            errors.put("status", Messages.ROOM_STATUS_INVALID);
        }
        int rows = parseInt(form.getRows());
        int cols = parseInt(form.getCols());
        Map<String, Seat> layout = null;
        if (rows < 1 || rows > MAX_ROWS || cols < 1 || cols > MAX_COLS) {
            errors.put("size", Messages.format(Messages.ROOM_SIZE_INVALID, MAX_ROWS, MAX_COLS));
        } else {
            // E1: sai kích thước/ký tự, hoặc ghế đôi không chiếm đủ 2 ô liền nhau (ghế chồng nhau) thì không dựng được lưới
            layout = parseLayout(form.getLayout(), rows, cols);
            if (layout == null) {
                errors.put("layout", Messages.ROOM_LAYOUT_INVALID);
            } else if (layout.values().stream().noneMatch(s -> RoomStatus.ACTIVE.equals(s.getStatus()))) {
                errors.put("layout", Messages.ROOM_LAYOUT_EMPTY);
            }
        }
        Map<String, Seat> existing = new LinkedHashMap<>();
        if (current != null) {
            for (Seat seat : roomDAO.findSeats(current.getRoomId())) {
                existing.put(seat.getRowName() + seat.getColNumber(), seat);
            }
            if (layout != null && !errors.containsKey("layout")) {
                checkSeatChanges(current.getRoomId(), existing, layout, errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Room room = current == null ? new Room() : current;
        room.setRoomName(form.getName());
        room.setFormat(form.getFormat());
        // Sức chứa: ghế đôi tính 2 người
        room.setTotalSeats(layout.values().stream().mapToInt(s -> SeatType.capacityOf(s.getSeatType())).sum());
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int roomId;
                if (current == null) {
                    room.setStatus(status);
                    roomId = roomDAO.insertRoom(conn, room);
                } else {
                    roomId = room.getRoomId();
                    roomDAO.updateRoom(conn, room);
                }
                applySeatChanges(conn, roomId, existing, layout);
                conn.commit();
                return roomId;
            } catch (SQLException e) {
                conn.rollback();
                throw translate(e);
            }
        }
    }

    // Form Sửa: tên, định dạng, trạng thái của phòng có sẵn (không đụng sơ đồ ghế).
    // Trạng thái: Bảo trì chỉ do UC-26 bật/tắt nên khi phòng đang Bảo trì thì giữ nguyên; Manager chọn
    // Hoạt động / Dừng hoạt động, và không dừng hoạt động phòng còn suất chiếu sắp tới.
    public void saveInfo(RoomLayoutForm form) throws SQLException, ValidationException {
        Room current = form.getRoomId() > 0 ? roomDAO.findRoom(form.getRoomId()) : null;
        if (current == null) {
            throw new ValidationException(Map.of("general", Messages.ROOM_NOT_FOUND));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        validateInfo(form, current, errors);
        String status = form.getStatus() == null ? "" : form.getStatus().trim();
        if (RoomStatus.MAINTENANCE.equals(current.getStatus())) {
            if (!status.isEmpty() && !RoomStatus.MAINTENANCE.equals(status)) {
                errors.put("status", Messages.ROOM_STATUS_MAINTENANCE_LOCKED);
            }
            status = RoomStatus.MAINTENANCE;
        } else if (!MANAGER_STATUSES.contains(status)) {
            errors.put("status", Messages.ROOM_STATUS_INVALID);
        } else if (RoomStatus.INACTIVE.equals(status) && !status.equals(current.getStatus())) {
            int upcoming = roomDAO.countUpcomingShowtimes(current.getRoomId());
            if (upcoming > 0) {
                errors.put("status", Messages.format(Messages.ROOM_STATUS_INACTIVE_LOCKED, upcoming));
            }
        }
        form.setStatus(status);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String expectedStatus = current.getStatus();
        current.setRoomName(form.getName());
        current.setFormat(form.getFormat());
        current.setStatus(status);
        try {
            if (!roomDAO.updateRoomInfo(current, expectedStatus)) {
                throw new ValidationException(Map.of("general", Messages.ROOM_STATUS_CHANGED));
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    // Tên (bắt buộc, tối đa 50, không trùng) và định dạng; phòng đã có định dạng thì không đổi khi còn suất chiếu sắp tới.
    // Chuẩn hóa (trim) tên/định dạng trong form để lưu và hiển thị lại.
    private void validateInfo(RoomLayoutForm form, Room current, Map<String, String> errors) throws SQLException {
        String name = form.getName() == null ? "" : form.getName().trim();
        String format = form.getFormat() == null ? "" : form.getFormat().trim();
        form.setName(name);
        form.setFormat(format);
        if (name.isEmpty()) {
            errors.put("name", Messages.format(Messages.MSG02, "Tên phòng"));
        } else if (name.length() > MAX_NAME) {
            errors.put("name", Messages.format(Messages.MSG08, MAX_NAME));
        } else if (roomDAO.existsByName(name, current == null ? 0 : current.getRoomId())) {
            errors.put("name", Messages.ROOM_NAME_DUPLICATE);
        }
        if (!MovieService.FORMATS.contains(format)) {
            errors.put("format", Messages.ROOM_FORMAT_INVALID);
        } else if (current != null && current.getFormat() != null && !format.equals(current.getFormat())) {
            // Phòng chưa khai báo định dạng (NULL) thì được điền lần đầu; chỉ chặn khi đổi định dạng đã có
            int upcoming = roomDAO.countUpcomingShowtimes(current.getRoomId());
            if (upcoming > 0) {
                errors.put("format", Messages.format(Messages.ROOM_FORMAT_LOCKED, upcoming));
            }
        }
    }

    // Business rules khi sửa sơ đồ của phòng đã có ghế:
    // - ghế đã có vé/giữ chỗ/báo cáo không xóa được (chỉ được khóa)
    // - ghế đã bán vé cho suất chưa kết thúc không bị khóa, không đổi loại (vé đã bán giữ nguyên số chỗ và giá)
    // - ghế có báo cáo sự cố đang mở phải giữ trạng thái khóa (UC-26 tự mở khi xử lý xong)
    private void checkSeatChanges(int roomId, Map<String, Seat> existing, Map<String, Seat> layout,
                                  Map<String, String> errors) throws SQLException {
        Set<Integer> history = roomDAO.findSeatIdsWithHistory(roomId);
        Set<Integer> sold = roomDAO.findSeatIdsWithUpcomingTickets(roomId);
        Set<Integer> reports = roomDAO.findSeatIdsWithOpenReports(roomId);
        List<String> removeLocked = new ArrayList<>();
        List<String> disableLocked = new ArrayList<>();
        List<String> typeLocked = new ArrayList<>();
        List<String> reportLocked = new ArrayList<>();
        for (Map.Entry<String, Seat> entry : existing.entrySet()) {
            Seat old = entry.getValue();
            Seat updated = layout.get(entry.getKey());
            if (updated == null) {
                if (history.contains(old.getSeatId())) {
                    removeLocked.add(entry.getKey());
                }
                continue;
            }
            boolean nowActive = RoomStatus.ACTIVE.equals(updated.getStatus());
            boolean wasActive = RoomStatus.ACTIVE.equals(old.getStatus());
            if (wasActive && !nowActive && sold.contains(old.getSeatId())) {
                disableLocked.add(entry.getKey());
            }
            if (!updated.getSeatType().equals(old.getSeatType()) && sold.contains(old.getSeatId())) {
                typeLocked.add(entry.getKey());
            }
            if (nowActive && reports.contains(old.getSeatId())) {
                reportLocked.add(entry.getKey());
            }
        }
        if (!removeLocked.isEmpty()) {
            errors.put("seatRemove", Messages.format(Messages.ROOM_SEAT_REMOVE_LOCKED, listPositions(removeLocked)));
        }
        if (!disableLocked.isEmpty()) {
            errors.put("seatDisable", Messages.format(Messages.ROOM_SEAT_DISABLE_LOCKED, listPositions(disableLocked)));
        }
        if (!typeLocked.isEmpty()) {
            errors.put("seatType", Messages.format(Messages.ROOM_SEAT_TYPE_LOCKED, listPositions(typeLocked)));
        }
        if (!reportLocked.isEmpty()) {
            errors.put("seatReport", Messages.format(Messages.ROOM_SEAT_REPORT_LOCKED, listPositions(reportLocked)));
        }
    }

    // Giữ nguyên SeatID của vị trí cũ (vé đã bán vẫn trỏ đúng ghế), thêm vị trí mới, xóa vị trí bị bỏ
    private void applySeatChanges(Connection conn, int roomId, Map<String, Seat> existing, Map<String, Seat> layout)
            throws SQLException {
        List<Seat> toInsert = new ArrayList<>();
        List<Seat> toUpdate = new ArrayList<>();
        List<Integer> toDelete = new ArrayList<>();
        for (Map.Entry<String, Seat> entry : layout.entrySet()) {
            Seat seat = entry.getValue();
            Seat old = existing.get(entry.getKey());
            if (old == null) {
                toInsert.add(seat);
            } else if (!seat.getSeatType().equals(old.getSeatType()) || !seat.getStatus().equals(old.getStatus())) {
                seat.setSeatId(old.getSeatId());
                toUpdate.add(seat);
            }
        }
        for (Map.Entry<String, Seat> entry : existing.entrySet()) {
            if (!layout.containsKey(entry.getKey())) {
                toDelete.add(entry.getValue().getSeatId());
            }
        }
        roomDAO.deleteSeats(conn, toDelete);
        roomDAO.updateSeats(conn, toUpdate);
        roomDAO.insertSeats(conn, roomId, toInsert);
    }

    // Vị trí ô trái -> ghế (chưa có SeatID); null nếu chuỗi sai kích thước, có ký tự lạ,
    // hoặc ghế đôi không có đúng một ô "=" ngay bên phải (ghế đôi ở cột cuối, "=" đứng riêng)
    private Map<String, Seat> parseLayout(String layout, int rows, int cols) {
        if (layout == null) {
            return null;
        }
        String[] lines = layout.trim().split(ROW_SEPARATOR, -1);
        if (lines.length != rows) {
            return null;
        }
        Map<String, Seat> seats = new LinkedHashMap<>();
        for (int r = 0; r < rows; r++) {
            if (lines[r].length() != cols) {
                return null;
            }
            for (int c = 0; c < cols; c++) {
                char code = lines[r].charAt(c);
                if (code == AISLE) {
                    continue;
                }
                String type = TYPE_BY_CODE.get(Character.toUpperCase(code));
                if (type == null) {
                    return null;
                }
                int leftCol = c;
                if (SeatType.widthOf(type) == 2) {
                    if (c + 1 >= cols || lines[r].charAt(c + 1) != COUPLE_RIGHT) {
                        return null;
                    }
                    c++;
                }
                Seat seat = new Seat();
                seat.setRowName(rowName(r));
                seat.setColNumber(leftCol + 1);
                seat.setSeatType(type);
                seat.setStatus(Character.isUpperCase(code) ? RoomStatus.ACTIVE : RoomStatus.MAINTENANCE);
                seats.put(seat.getRowName() + seat.getColNumber(), seat);
            }
        }
        return seats;
    }

    // Tranh chấp lúc lưu: trùng tên (UNIQUE) hoặc ghế vừa phát sinh vé/giữ chỗ (khóa ngoại khi xóa)
    private SQLException translate(SQLException e) throws ValidationException {
        int code = e.getErrorCode();
        if (code == SQL_UNIQUE_VIOLATION || code == SQL_UNIQUE_INDEX_VIOLATION) {
            throw new ValidationException(Map.of("name", Messages.ROOM_NAME_DUPLICATE));
        }
        if (code == SQL_FOREIGN_KEY_VIOLATION) {
            throw new ValidationException(Map.of("general", Messages.ROOM_LAYOUT_CHANGED));
        }
        return e;
    }

    private String listPositions(Collection<String> positions) {
        List<String> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparing((String p) -> p.charAt(0)).thenComparingInt(p -> Integer.parseInt(p.substring(1))));
        if (sorted.size() <= MAX_LISTED) {
            return String.join(", ", sorted);
        }
        return String.join(", ", sorted.subList(0, MAX_LISTED)) + " (+" + (sorted.size() - MAX_LISTED) + ")";
    }

    private String filledLayout(int rows, int cols, char code) {
        String line = String.valueOf(code).repeat(cols);
        List<String> lines = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            lines.add(line);
        }
        return String.join(ROW_SEPARATOR, lines);
    }

    private ValidationException unsupportedLayout() {
        return new ValidationException(Map.of("general", Messages.format(Messages.ROOM_LAYOUT_UNSUPPORTED, MAX_COLS)));
    }

    private char codeOf(String seatType) {
        return CODE_BY_TYPE.getOrDefault(seatType, 'S');
    }

    // Hàng A-Z -> 0-25; -1 nếu không theo chuẩn
    private int rowIndex(String rowName) {
        return rowName != null && rowName.length() == 1 && rowName.charAt(0) >= 'A' && rowName.charAt(0) <= 'Z'
                ? rowName.charAt(0) - 'A' : -1;
    }

    private String rowName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
