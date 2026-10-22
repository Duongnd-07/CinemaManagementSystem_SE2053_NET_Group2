package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.ShowtimeDAO;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.DBContext;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

public class ShowtimeService {
    // UC-08: mặc định xem hôm nay và 7 ngày tới
    public static final int SCHEDULE_DAYS_AHEAD = 7;
    // Nhóm cho phòng chưa khai báo định dạng (Room.Format cho phép NULL)
    private static final String OTHER_FORMAT = "Khác";

    public static final int MANAGER_PAGE_SIZE = 10;

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    // UC-06: nút Đặt vé chỉ hiện khi phim còn suất chiếu sắp tới; dùng cùng khung với trang lịch chiếu
    // để không dẫn khách tới trang lịch chiếu trống
    public boolean hasBookableShowtimes(int movieId) throws SQLException {
        return !findInWindow(movieId).isEmpty();
    }

    // UC-08: lịch chiếu của phim trong khung hôm nay + 7 ngày, lọc theo ngày/định dạng/phòng (rỗng = tất cả),
    // nhóm Ngày -> Định dạng -> Phòng. Chỉ suất chưa bắt đầu nên suất đã qua của hôm nay tự bị ẩn.
    public ShowtimeSchedule getSchedule(int movieId, String date, String format, String roomId) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDate lastDay = today.plusDays(SCHEDULE_DAYS_AHEAD);
        List<Showtime> showtimes = findInWindow(movieId);

        ShowtimeSchedule schedule = new ShowtimeSchedule();
        for (LocalDate day = today; !day.isAfter(lastDay); day = day.plusDays(1)) {
            schedule.getDays().add(day);
            schedule.getCountByDay().put(day, 0);
        }
        schedule.setFormats(formatsOf(showtimes));
        schedule.setRooms(roomsOf(showtimes));

        // Lựa chọn không hợp lệ (sai định dạng, ngoài khung, không có suất) thì bỏ qua bộ lọc đó
        LocalDate selectedDate = parseDate(date);
        schedule.setSelectedDate(schedule.getDays().contains(selectedDate) ? selectedDate : null);
        String selectedFormat = format == null ? "" : format.trim();
        schedule.setSelectedFormat(schedule.getFormats().contains(selectedFormat) ? selectedFormat : "");
        int selectedRoom = parseInt(roomId);
        boolean roomExists = schedule.getRooms().stream().anyMatch(r -> r.getRoomId() == selectedRoom);
        schedule.setSelectedRoomId(roomExists ? selectedRoom : 0);

        for (Showtime showtime : showtimes) {
            Room room = showtime.getRoom();
            if (!schedule.getSelectedFormat().isEmpty() && !schedule.getSelectedFormat().equals(room.getFormat())) {
                continue;
            }
            if (schedule.getSelectedRoomId() > 0 && schedule.getSelectedRoomId() != room.getRoomId()) {
                continue;
            }
            LocalDate day = showtime.getStartTime().toLocalDate();
            schedule.getCountByDay().merge(day, 1, Integer::sum);
            if (schedule.getSelectedDate() != null && !schedule.getSelectedDate().equals(day)) {
                continue;
            }
            schedule.getGroups()
                    .computeIfAbsent(day, d -> new TreeMap<>(Comparator.comparingInt(this::formatOrder)
                            .thenComparing(Comparator.naturalOrder())))
                    .computeIfAbsent(room.getFormat() == null ? OTHER_FORMAT : room.getFormat(), f -> new TreeMap<>())
                    .computeIfAbsent(room.getRoomName(), r -> new ArrayList<>())
                    .add(showtime);
        }
        return schedule;
    }

    // Suất đang mở bán, bắt đầu từ bây giờ tới hết ngày thứ 7 kể từ hôm nay, của phim đang công khai
    private List<Showtime> findInWindow(int movieId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.toLocalDate().plusDays(SCHEDULE_DAYS_AHEAD + 1L).atStartOfDay();
        return showtimeDAO.findActiveByMovie(movieId, now, end, MovieStatus.PUBLIC);
    }

    // Định dạng theo thứ tự 2D, 3D, IMAX rồi tới định dạng khác (nếu có)
    private List<String> formatsOf(List<Showtime> showtimes) {
        List<String> formats = new ArrayList<>();
        for (Showtime showtime : showtimes) {
            String format = showtime.getRoom().getFormat();
            if (format != null && !formats.contains(format)) {
                formats.add(format);
            }
        }
        formats.sort(Comparator.comparingInt(this::formatOrder).thenComparing(Comparator.naturalOrder()));
        return formats;
    }

    private List<Room> roomsOf(List<Showtime> showtimes) {
        Map<Integer, Room> rooms = new LinkedHashMap<>();
        for (Showtime showtime : showtimes) {
            rooms.putIfAbsent(showtime.getRoom().getRoomId(), showtime.getRoom());
        }
        List<Room> result = new ArrayList<>(rooms.values());
        result.sort(Comparator.comparing(Room::getRoomName));
        return result;
    }

    private int formatOrder(String format) {
        int index = MovieService.FORMATS.indexOf(format);
        return index < 0 ? MovieService.FORMATS.size() : index;
    }

    // =========================================================================
    // UC-24: Manage Showtimes (Dành cho Quản lý / Manager)
    // =========================================================================

    /**
     * Danh sách suất chiếu phân trang kèm bộ lọc (theo phim, phòng, ngày, trạng thái).
     */
    public Page<Showtime> listManager(Integer movieId, Integer roomId, String dateStr, String status, int page)
            throws SQLException {
        LocalDate date = parseDate(dateStr);
        String normalizedStatus = (status != null && ShowtimeStatus.isValid(status)) ? status : null;
        int total = showtimeDAO.count(movieId, roomId, date, normalizedStatus);
        return Page.of(page, MANAGER_PAGE_SIZE, total,
                (offset, limit) -> showtimeDAO.search(movieId, roomId, date, normalizedStatus, offset, limit));
    }

    /**
     * Thống kê số lượng suất chiếu theo trạng thái để phục vụ các tab đếm trên giao diện.
     */
    public Map<String, Integer> countByStatus(Integer movieId, Integer roomId, String dateStr) throws SQLException {
        LocalDate date = parseDate(dateStr);
        return showtimeDAO.countByStatus(movieId, roomId, date);
    }

    /**
     * Lấy thông tin chi tiết của một suất chiếu theo ID.
     */
    public Showtime findById(int showtimeId) throws SQLException {
        return showtimeDAO.findById(showtimeId);
    }

    /**
     * Lấy danh sách phim đang hoặc sắp chiếu phục vụ dropdown tạo suất chiếu.
     */
    public List<Movie> findPublicMovies() throws SQLException {
        return movieDAO.findByStatuses(MovieStatus.PUBLIC, null, null, null, null, null);
    }

    /**
     * Lấy danh sách tất cả phòng chiếu phục vụ dropdown lọc và tạo suất chiếu.
     */
    public List<Room> findAllRooms() throws SQLException {
        return roomDAO.findAll();
    }

    /**
     * Lưu suất chiếu (thêm mới hoặc cập nhật).
     * Áp dụng thuật toán kiểm tra chống trùng phòng GB-08 và thuật toán tính giờ kết thúc.
     */
    public void save(Integer showtimeId, ShowtimeForm form) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Kiểm tra Phim
        int movieId = parseInt(form.getMovieId());
        Movie movie = null;
        if (movieId <= 0) {
            errors.put("movieId", Messages.SHOWTIME_MOVIE_INVALID);
        } else {
            movie = movieDAO.findById(movieId);
            if (movie == null || !MovieStatus.PUBLIC.contains(movie.getStatus())) {
                errors.put("movieId", Messages.SHOWTIME_MOVIE_INVALID);
            }
        }

        // 2. Kiểm tra Phòng chiếu (chặn hoàn toàn phòng Dừng hoạt động hoặc Bảo trì)
        int roomId = parseInt(form.getRoomId());
        Room room = null;
        if (roomId <= 0) {
            errors.put("roomId", Messages.SHOWTIME_ROOM_INVALID);
        } else {
            room = roomDAO.findById(roomId);
            if (room == null) {
                errors.put("roomId", Messages.SHOWTIME_ROOM_INVALID);
            } else if (RoomStatus.INACTIVE.equals(room.getStatus())) {
                errors.put("roomId", Messages.SHOWTIME_ROOM_INACTIVE);
            } else if (RoomStatus.MAINTENANCE.equals(room.getStatus())) {
                errors.put("roomId", Messages.SHOWTIME_ROOM_MAINTENANCE);
            } else if (!RoomStatus.ACTIVE.equals(room.getStatus())) {
                errors.put("roomId", Messages.SHOWTIME_ROOM_INVALID);
            }
        }

        // 3. Kiểm tra Thời gian bắt đầu (phải ở tương lai)
        LocalDateTime startTime = null;
        if (form.getStartTime() == null || form.getStartTime().trim().isEmpty()) {
            errors.put("startTime", Messages.SHOWTIME_START_TIME_REQUIRED);
        } else {
            try {
                startTime = LocalDateTime.parse(form.getStartTime().trim());
                if (!startTime.isAfter(LocalDateTime.now())) {
                    errors.put("startTime", Messages.SHOWTIME_START_TIME_PAST);
                }
            } catch (DateTimeParseException e) {
                errors.put("startTime", Messages.SHOWTIME_START_TIME_REQUIRED);
            }
        }

        // 4. Kiểm tra Giá vé gốc (>= 10,000 VND và là bội số của 1,000)
        BigDecimal basePrice = null;
        if (form.getBasePrice() == null || form.getBasePrice().trim().isEmpty()) {
            errors.put("basePrice", Messages.SHOWTIME_PRICE_INVALID);
        } else {
            try {
                basePrice = new BigDecimal(form.getBasePrice().trim());
                if (basePrice.compareTo(new BigDecimal("10000")) < 0
                        || basePrice.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) != 0) {
                    errors.put("basePrice", Messages.SHOWTIME_PRICE_INVALID);
                }
            } catch (NumberFormatException e) {
                errors.put("basePrice", Messages.SHOWTIME_PRICE_INVALID);
            }
        }

        // 5. Kiểm tra tính bất biến nếu suất chiếu đã phát sinh đặt vé / giữ chỗ
        if (showtimeId != null && showtimeId > 0) {
            Showtime existing = showtimeDAO.findById(showtimeId);
            if (existing == null) {
                errors.put("general", Messages.SHOWTIME_NOT_FOUND);
            } else if (showtimeDAO.hasBookings(showtimeId)) {
                boolean movieChanged = existing.getMovieId() != movieId;
                boolean roomChanged = existing.getRoomId() != roomId;
                boolean timeChanged = startTime != null && !existing.getStartTime().equals(startTime);
                if (movieChanged || roomChanged || timeChanged) {
                    errors.put("general", Messages.SHOWTIME_IMMUTABLE_BOOKED);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 6. GB-08: Tính Giờ kết thúc = StartTime + Movie Duration + 15 phút dọn rạp
        LocalDateTime endTime = ShowtimePricing.calculateEndTime(startTime, movie.getDurationMinutes());

        // 7. GB-08: Thuật toán chống trùng phòng chiếu
        List<Showtime> collisions = showtimeDAO.findCollisions(roomId, startTime, endTime, showtimeId);
        if (!collisions.isEmpty()) {
            Showtime c = collisions.get(0);
            String colMovieTitle = c.getMovie() != null ? c.getMovie().getTitle() : "Phim khác";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String msg = Messages.format(Messages.SHOWTIME_COLLISION,
                    room.getRoomName(), colMovieTitle, c.getStartTime().format(dtf), c.getEndTime().format(dtf));
            errors.put("collision", msg);
            throw new ValidationException(errors);
        }

        // 8. Lưu vào DB
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(showtimeId != null ? showtimeId : 0);
        showtime.setMovieId(movieId);
        showtime.setRoomId(roomId);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setBasePrice(basePrice);
        showtime.setStatus(form.getStatus() != null && ShowtimeStatus.isValid(form.getStatus())
                ? form.getStatus() : ShowtimeStatus.ACTIVE);

        try (Connection conn = DBContext.getConnection()) {
            if (showtimeId == null || showtimeId <= 0) {
                showtimeDAO.insert(conn, showtime);
            } else {
                showtimeDAO.update(conn, showtime);
            }
        }
    }

    /**
     * Hủy suất chiếu (đổi trạng thái sang Cancelled).
     */
    public void cancel(int showtimeId) throws SQLException, ValidationException {
        Showtime existing = showtimeDAO.findById(showtimeId);
        if (existing == null) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_NOT_FOUND));
        }
        if (ShowtimeStatus.CANCELLED.equals(existing.getStatus())) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_CANCEL_ALREADY));
        }
        showtimeDAO.cancel(showtimeId);
    }

    /**
     * Xóa suất chiếu khỏi DB nếu chưa có vé đặt.
     */
    public void delete(int showtimeId) throws SQLException, ValidationException {
        Showtime existing = showtimeDAO.findById(showtimeId);
        if (existing == null) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_NOT_FOUND));
        }
        if (showtimeDAO.hasBookings(showtimeId)) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_DELETE_HAS_BOOKINGS));
        }
        showtimeDAO.delete(showtimeId);
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
