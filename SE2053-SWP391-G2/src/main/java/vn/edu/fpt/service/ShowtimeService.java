package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.dao.RoomDAO;
import vn.edu.fpt.dao.ShowtimeDAO;
import vn.edu.fpt.dao.SystemConfigDAO;
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
    private final SystemConfigDAO configDAO = new SystemConfigDAO();

    // UC-06: nút Đặt vé chỉ hiện khi phim còn suất chiếu sắp tới; dùng cùng khung với trang lịch chiếu
    // để không dẫn khách tới trang lịch chiếu trống
    public boolean hasBookableShowtimes(int movieId) throws SQLException {
        return !findInWindow(movieId).isEmpty();
    }

    // UC-08: lịch chiếu của phim trong khung hôm nay + 7 ngày, lọc theo ngày/định dạng/phòng (rỗng = tất cả),
    // nhóm Ngày -> Định dạng -> Phòng. Chỉ suất chưa bắt đầu nên suất đã qua của hôm nay tự bị ẩn.
    // UC-08: lịch chiếu của phim trong khung hôm nay + 7 ngày, lọc theo ngày/định dạng/phòng/bản chiếu (rỗng = tất cả),
    // nhóm Ngày -> Định dạng -> Phòng. Chỉ suất chưa bắt đầu nên suất đã qua của hôm nay tự bị ẩn.
    public ShowtimeSchedule getSchedule(int movieId, String date, String format, String roomId) throws SQLException {
        return getSchedule(movieId, date, format, roomId, null);
    }

    public ShowtimeSchedule getSchedule(int movieId, String date, String format, String roomId, String translationType)
            throws SQLException {
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
        schedule.setTranslationTypes(translationTypesOf(showtimes));

        // Lựa chọn không hợp lệ (sai định dạng, ngoài khung, không có suất) thì bỏ qua bộ lọc đó
        LocalDate selectedDate = parseDate(date);
        schedule.setSelectedDate(schedule.getDays().contains(selectedDate) ? selectedDate : null);
        String selectedFormat = format == null ? "" : format.trim();
        schedule.setSelectedFormat(schedule.getFormats().contains(selectedFormat) ? selectedFormat : "");
        int selectedRoom = parseInt(roomId);
        boolean roomExists = schedule.getRooms().stream().anyMatch(r -> r.getRoomId() == selectedRoom);
        schedule.setSelectedRoomId(roomExists ? selectedRoom : 0);
        String selectedTrans = translationType == null ? "" : translationType.trim();
        schedule.setSelectedTranslationType(schedule.getTranslationTypes().contains(selectedTrans) ? selectedTrans : "");

        for (Showtime showtime : showtimes) {
            Room room = showtime.getRoom();
            if (!schedule.getSelectedFormat().isEmpty() && !schedule.getSelectedFormat().equals(room.getFormat())) {
                continue;
            }
            if (schedule.getSelectedRoomId() > 0 && schedule.getSelectedRoomId() != room.getRoomId()) {
                continue;
            }
            if (!schedule.getSelectedTranslationType().isEmpty()
                    && !schedule.getSelectedTranslationType().equalsIgnoreCase(showtime.getTranslationType())) {
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

    // Suất đang mở bán online (chưa tới giờ đóng bán ONLINE_SALE_CUTOFF_MINUTES trước giờ chiếu - UC-13),
    // tới hết ngày thứ 7 kể từ hôm nay, của phim đang công khai
    private List<Showtime> findInWindow(int movieId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        int cutoffMinutes = Math.max(0, configDAO.getInt(BookingService.CONFIG_SALE_CUTOFF,
                BookingService.DEFAULT_SALE_CUTOFF_MINUTES));
        LocalDateTime end = now.toLocalDate().plusDays(SCHEDULE_DAYS_AHEAD + 1L).atStartOfDay();
        return showtimeDAO.findActiveByMovie(movieId, now.plusMinutes(cutoffMinutes), end, MovieStatus.PUBLIC);
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

    private List<String> translationTypesOf(List<Showtime> showtimes) {
        List<String> types = new ArrayList<>();
        for (Showtime showtime : showtimes) {
            String type = showtime.getTranslationType();
            if (type != null && !type.trim().isEmpty() && !types.contains(type.trim())) {
                types.add(type.trim());
            }
        }
        return types;
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
        return listManager(movieId, roomId, dateStr, status, null, page);
    }

    public Page<Showtime> listManager(Integer movieId, Integer roomId, String dateStr, String status,
                                      String translationType, int page) throws SQLException {
        LocalDate date = parseDate(dateStr);
        String normalizedStatus = (status != null && ShowtimeStatus.isValid(status)) ? status : null;
        int total = showtimeDAO.count(movieId, roomId, date, normalizedStatus, translationType);
        return Page.of(page, MANAGER_PAGE_SIZE, total,
                (offset, limit) -> showtimeDAO.search(movieId, roomId, date, normalizedStatus, translationType, offset, limit));
    }

    /**
     * Thống kê số lượng suất chiếu theo trạng thái để phục vụ các tab đếm trên giao diện.
     */
    public Map<String, Integer> countByStatus(Integer movieId, Integer roomId, String dateStr) throws SQLException {
        return countByStatus(movieId, roomId, dateStr, null);
    }

    public Map<String, Integer> countByStatus(Integer movieId, Integer roomId, String dateStr, String translationType)
            throws SQLException {
        LocalDate date = parseDate(dateStr);
        return showtimeDAO.countByStatus(movieId, roomId, date, translationType);
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
     * Lưu 1 suất chiếu (thêm mới hoặc cập nhật).
     * Áp dụng thuật toán kiểm tra chống trùng phòng GB-08, kiểm tra tương thích định dạng phim,
     * ngày khởi chiếu, khóa bất biến khi đã có vé/giữ chỗ, và thực thi trong 1 transaction an toàn.
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
        Room room = roomId > 0 ? roomDAO.findById(roomId) : null;
        String roomError = roomStatusError(room);
        if (roomError != null) {
            errors.put("roomId", roomError);
        }

        // 3. Kiểm tra tính tương thích định dạng phòng chiếu vs định dạng phim (Góp ý 5)
        if (movie != null && room != null) {
            String mismatch = formatMismatch(movie, room);
            if (mismatch != null) {
                errors.put("roomId", mismatch);
            }
        }

        // 4. Kiểm tra Thời gian bắt đầu
        LocalDateTime startTime = null;
        if (form.getStartTime() == null || form.getStartTime().trim().isEmpty()) {
            errors.put("startTime", Messages.SHOWTIME_START_TIME_REQUIRED);
        } else {
            try {
                startTime = LocalDateTime.parse(form.getStartTime().trim());
            } catch (DateTimeParseException e) {
                errors.put("startTime", Messages.SHOWTIME_START_TIME_REQUIRED);
            }
        }
        // UC-23: không xếp suất sau ngày kết thúc chiếu của phim (nếu đã đặt)
        if (startTime != null && !errors.containsKey("startTime") && movie != null && movie.getEndDate() != null
                && startTime.toLocalDate().isAfter(movie.getEndDate())) {
            errors.put("startTime", Messages.format(Messages.SHOWTIME_AFTER_MOVIE_END,
                    movie.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }

        // 5. Kiểm tra Giá vé gốc (>= 10,000 VND và là bội số của 1,000)
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

        // 6. Kiểm tra khi Chỉnh sửa suất chiếu hiện có
        Showtime existing = null;
        if (showtimeId != null && showtimeId > 0) {
            existing = showtimeDAO.findById(showtimeId);
            if (existing == null) {
                errors.put("general", Messages.SHOWTIME_NOT_FOUND);
            } else {
                // Nếu giờ bắt đầu bị thay đổi thì giờ mới phải ở tương lai;
                // Nếu giờ bắt đầu không đổi (chỉ sửa giá vé), cho phép giữ nguyên kể cả suất sắp chiếu (Góp ý 10)
                if (startTime != null && !existing.getStartTime().equals(startTime)) {
                    if (!startTime.isAfter(LocalDateTime.now())) {
                        errors.put("startTime", Messages.SHOWTIME_START_TIME_PAST);
                    }
                }
                // Nếu đã có vé bán hoặc giữ chỗ: khóa hoàn toàn việc đổi phim, phòng, hoặc giờ chiếu
                if (showtimeDAO.hasBookings(showtimeId)) {
                    boolean movieChanged = existing.getMovieId() != movieId;
                    boolean roomChanged = existing.getRoomId() != roomId;
                    boolean timeChanged = startTime != null && !existing.getStartTime().equals(startTime);
                    if (movieChanged || roomChanged || timeChanged) {
                        errors.put("general", Messages.SHOWTIME_IMMUTABLE_BOOKED);
                    }
                }
            }
        } else {
            // Khi tạo mới: bắt buộc giờ bắt đầu phải ở tương lai
            if (startTime != null && !startTime.isAfter(LocalDateTime.now())) {
                errors.put("startTime", Messages.SHOWTIME_START_TIME_PAST);
            }
        }

        // 7. Kiểm tra ngày khởi chiếu của phim (Góp ý 11)
        if (movie != null && movie.getReleaseDate() != null && startTime != null) {
            if (startTime.toLocalDate().isBefore(movie.getReleaseDate())) {
                errors.put("startTime", Messages.format(Messages.SHOWTIME_BEFORE_RELEASE_DATE,
                        startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        movie.getReleaseDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 8. GB-08: Tính Giờ kết thúc = StartTime + Movie Duration + 15 phút dọn rạp
        LocalDateTime endTime = ShowtimePricing.calculateEndTime(startTime, movie.getDurationMinutes());

        // 9. Thực thi kiểm tra trùng phòng và lưu trong cùng 1 Transaction để chống Race Condition (Góp ý 8)
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Showtime> collisions = showtimeDAO.findCollisions(conn, roomId, startTime, endTime, showtimeId);
                if (!collisions.isEmpty()) {
                    Showtime c = collisions.get(0);
                    String colMovieTitle = c.getMovie() != null ? c.getMovie().getTitle() : "Phim khác";
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    String msg = Messages.format(Messages.SHOWTIME_COLLISION,
                            room.getRoomName(), colMovieTitle, c.getStartTime().format(dtf), c.getEndTime().format(dtf));
                    errors.put("collision", msg);
                    throw new ValidationException(errors);
                }

                Showtime showtime = new Showtime();
                showtime.setShowtimeId(showtimeId != null ? showtimeId : 0);
                showtime.setMovieId(movieId);
                showtime.setRoomId(roomId);
                showtime.setStartTime(startTime);
                showtime.setEndTime(endTime);
                showtime.setBasePrice(basePrice);
                String translationType = form.getTranslationType();
                if (translationType == null || translationType.isBlank() || (!translationType.equals("Phụ đề") && !translationType.equals("Lồng tiếng"))) {
                    translationType = "Phụ đề";
                }
                showtime.setTranslationType(translationType);

                if (showtimeId == null || showtimeId <= 0) {
                    // Tạo mới luôn là Active (Góp ý 4)
                    showtime.setStatus(ShowtimeStatus.ACTIVE);
                    showtimeDAO.insert(conn, showtime);
                } else {
                    // Sửa thông tin giữ nguyên trạng thái cũ của suất chiếu (Góp ý 4)
                    showtime.setStatus(existing != null ? existing.getStatus() : ShowtimeStatus.ACTIVE);
                    showtimeDAO.update(conn, showtime);
                }

                conn.commit();
            } catch (SQLException | ValidationException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Tạo 1 suất chiếu (cùng phim, cùng giờ bắt đầu) đồng thời ở nhiều phòng.
     * Giá vé để trống thì mỗi phòng tự tính theo giá gợi ý (ngày/khung giờ/định dạng phòng).
     * Chỉ lưu khi mọi phòng đều hợp lệ và không trùng lịch: kiểm tra và lưu trong 1 transaction.
     *
     * @return số suất chiếu đã tạo
     */
    public int createMany(ShowtimeForm form) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();

        Movie movie = validateMovie(form.getMovieId(), errors);
        List<Room> rooms = validateRooms(form.getRoomIds(), movie, errors);

        LocalDateTime startTime = null;
        try {
            if (!form.getStartTime().isEmpty()) {
                startTime = LocalDateTime.parse(form.getStartTime());
            }
        } catch (DateTimeParseException ignored) {
        }
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (startTime == null) {
            errors.put("startTime", Messages.SHOWTIME_START_TIME_REQUIRED);
        } else if (!startTime.isAfter(LocalDateTime.now())) {
            errors.put("startTime", Messages.SHOWTIME_START_TIME_PAST);
        } else if (movie != null && movie.getReleaseDate() != null
                && startTime.toLocalDate().isBefore(movie.getReleaseDate())) {
            errors.put("startTime", Messages.format(Messages.SHOWTIME_BEFORE_RELEASE_DATE,
                    startTime.format(dateFormat), movie.getReleaseDate().format(dateFormat)));
        } else if (movie != null && movie.getEndDate() != null
                && startTime.toLocalDate().isAfter(movie.getEndDate())) {
            errors.put("startTime", Messages.format(Messages.SHOWTIME_AFTER_MOVIE_END,
                    movie.getEndDate().format(dateFormat)));
        }

        BigDecimal customPrice = parseOptionalPrice(form.getBasePrice(), errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        LocalDateTime endTime = ShowtimePricing.calculateEndTime(startTime, movie.getDurationMinutes());
        String translationType = normalizeTranslationType(form.getTranslationType());

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Showtime> toInsert = new ArrayList<>();
                List<String> collisionRooms = new ArrayList<>();
                String singleCollision = null;
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                for (Room room : rooms) {
                    List<Showtime> collisions = showtimeDAO.findCollisions(conn, room.getRoomId(), startTime, endTime, null);
                    if (!collisions.isEmpty()) {
                        Showtime c = collisions.get(0);
                        String colMovieTitle = c.getMovie() != null ? c.getMovie().getTitle() : "Phim khác";
                        singleCollision = Messages.format(Messages.SHOWTIME_COLLISION, room.getRoomName(),
                                colMovieTitle, c.getStartTime().format(dtf), c.getEndTime().format(dtf));
                        collisionRooms.add(room.getRoomName() + " (\"" + colMovieTitle + "\" "
                                + c.getStartTime().format(dtf) + " - " + c.getEndTime().format(dtf) + ")");
                        continue;
                    }
                    BigDecimal price = customPrice != null ? customPrice
                            : ShowtimePricing.calculateSuggestedPrice(startTime, roomFormatOf(room));
                    toInsert.add(newShowtime(movie.getMovieId(), room.getRoomId(), startTime, endTime, price,
                            translationType));
                }
                if (!collisionRooms.isEmpty()) {
                    String message = collisionRooms.size() == 1 ? singleCollision
                            : Messages.format(Messages.SHOWTIME_COLLISION_MULTI, String.join("; ", collisionRooms));
                    throw new ValidationException(Map.of("collision", message));
                }

                showtimeDAO.insertBatch(conn, toInsert);
                conn.commit();
                return toInsert.size();
            } catch (SQLException | ValidationException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Lên lịch suất chiếu hàng loạt (Batch Scheduling - Góp ý 1) cho 1 phim ở 1 hoặc nhiều phòng.
     * Mỗi phòng nhận cùng bộ ngày/khung giờ. Toàn bộ các suất chiếu được kiểm tra chống trùng lịch
     * và lưu trong cùng 1 Transaction duy nhất.
     */
    public int saveBatch(BatchShowtimeForm form) throws SQLException, ValidationException {
        Map<String, String> errors = new LinkedHashMap<>();

        Movie movie = validateMovie(form.getMovieId(), errors);
        List<Room> rooms = validateRooms(form.getRoomIds(), movie, errors);

        // Validate Dates
        LocalDate startDate = parseDate(form.getStartDate());
        LocalDate endDate = parseDate(form.getEndDate());
        LocalDate today = LocalDate.now();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            errors.put("dates", Messages.SHOWTIME_BATCH_DATE_INVALID);
        } else if (startDate.isBefore(today)) {
            errors.put("dates", "Ngày bắt đầu không được là ngày trong quá khứ. Vui lòng chọn từ ngày hôm nay trở đi.");
        } else if (startDate.plusDays(31).isBefore(endDate)) {
            errors.put("dates", "Khoang thoi gian tao hang loat toi da la 31 ngay.");
        }

        // Validate Days of week
        if (form.getDaysOfWeek().isEmpty()) {
            errors.put("daysOfWeek", Messages.SHOWTIME_BATCH_DAYS_REQUIRED);
        }

        // Validate Time slots
        List<LocalTime> validSlots = new ArrayList<>();
        for (String slotStr : form.getTimeSlots()) {
            if (slotStr != null && !slotStr.trim().isEmpty()) {
                try {
                    validSlots.add(LocalTime.parse(slotStr.trim()));
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        if (validSlots.isEmpty()) {
            errors.put("timeSlots", Messages.SHOWTIME_BATCH_SLOTS_REQUIRED);
        }

        // Validate optional base price
        BigDecimal customBasePrice = parseOptionalPrice(form.getBasePrice(), errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Collections.sort(validSlots);
        int duration = movie.getDurationMinutes();
        String translationType = normalizeTranslationType(form.getTranslationType());

        // 1. Sinh danh sách giờ bắt đầu dự kiến (giống nhau cho mọi phòng đã chọn), theo thứ tự thời gian
        List<LocalDateTime> plannedStarts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            int dayVal = d.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
            if (!form.getDaysOfWeek().contains(dayVal)) {
                continue;
            }
            if (movie.getReleaseDate() != null && d.isBefore(movie.getReleaseDate())) {
                continue;
            }
            for (LocalTime slot : validSlots) {
                LocalDateTime start = LocalDateTime.of(d, slot);
                if (start.isAfter(now)) { // bỏ qua các mốc giờ trong quá khứ
                    plannedStarts.add(start);
                }
            }
        }

        if (plannedStarts.isEmpty()) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_BATCH_EMPTY));
        }

        // 2. Kiểm tra xung đột nội bộ giữa các khung giờ trong đợt (mỗi phòng dùng chung bộ khung giờ này)
        for (int i = 0; i < plannedStarts.size() - 1; i++) {
            LocalDateTime s1Start = plannedStarts.get(i);
            LocalDateTime s1End = ShowtimePricing.calculateEndTime(s1Start, duration);
            LocalDateTime s2Start = plannedStarts.get(i + 1);
            if (s1End.isAfter(s2Start)) {
                LocalDateTime s2End = ShowtimePricing.calculateEndTime(s2Start, duration);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM");
                String msg = "Xung đột thời gian giữa 2 suất trong đợt tạo: ["
                        + s1Start.format(dtf) + " - " + s1End.format(dtf)
                        + "] và [" + s2Start.format(dtf) + " - " + s2End.format(dtf)
                        + "]. Khoảng cách giữa 2 suất phải gồm " + duration + " phút phim + 15 phút dọn rạp.";
                throw new ValidationException(Map.of("general", msg));
            }
        }

        // 3. Thực hiện kiểm tra DB và lưu trong 1 TRANSACTION DUY NHẤT
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Showtime> toInsert = new ArrayList<>();
                for (Room room : rooms) {
                    String roomFormat = roomFormatOf(room);
                    for (LocalDateTime start : plannedStarts) {
                        LocalDateTime end = ShowtimePricing.calculateEndTime(start, duration);
                        List<Showtime> collisions = showtimeDAO.findCollisions(conn, room.getRoomId(), start, end, null);
                        if (!collisions.isEmpty()) {
                            if (form.isSkipCollisions()) {
                                continue; // Bỏ qua suất bị trùng
                            }
                            Showtime c = collisions.get(0);
                            String colMovieTitle = c.getMovie() != null ? c.getMovie().getTitle() : "Phim khác";
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM");
                            String msg = "Suất chiếu ngày " + start.format(dtf) + " tại " + room.getRoomName()
                                    + " bị trùng với suất đã có: \"" + colMovieTitle + "\" ("
                                    + c.getStartTime().format(dtf) + " - " + c.getEndTime().format(dtf) + ").";
                            throw new ValidationException(Map.of("general", msg));
                        }
                        BigDecimal price = customBasePrice != null ? customBasePrice
                                : ShowtimePricing.calculateSuggestedPrice(start, roomFormat);
                        toInsert.add(newShowtime(movie.getMovieId(), room.getRoomId(), start, end, price, translationType));
                    }
                }

                if (toInsert.isEmpty()) {
                    throw new ValidationException(Map.of("general", Messages.SHOWTIME_BATCH_EMPTY));
                }

                showtimeDAO.insertBatch(conn, toInsert);
                conn.commit();
                return toInsert.size();
            } catch (SQLException | ValidationException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Phim phải tồn tại và đang/sắp chiếu
    private Movie validateMovie(String movieIdValue, Map<String, String> errors) throws SQLException {
        int movieId = parseInt(movieIdValue);
        Movie movie = movieId > 0 ? movieDAO.findById(movieId) : null;
        if (movie == null || !MovieStatus.PUBLIC.contains(movie.getStatus())) {
            errors.put("movieId", Messages.SHOWTIME_MOVIE_INVALID);
            return null;
        }
        return movie;
    }

    // Tạo mới ở nhiều phòng: mỗi phòng phải đang hoạt động và hợp định dạng phim.
    // Lỗi của các phòng được gom vào 1 thông báo dưới ô chọn phòng; chỉ trả về các phòng hợp lệ
    private List<Room> validateRooms(List<String> roomIds, Movie movie, Map<String, String> errors)
            throws SQLException {
        List<Room> rooms = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (String value : roomIds) {
            int roomId = parseInt(value);
            Room room = roomId > 0 ? roomDAO.findById(roomId) : null;
            String statusError = roomStatusError(room);
            if (statusError != null) {
                problems.add(room == null ? statusError : room.getRoomName() + ": " + statusError);
                continue;
            }
            String mismatch = movie == null ? null : formatMismatch(movie, room);
            if (mismatch != null) {
                problems.add(mismatch);
                continue;
            }
            rooms.add(room);
        }
        if (roomIds.isEmpty()) {
            errors.put("roomId", Messages.SHOWTIME_ROOMS_REQUIRED);
        } else if (!problems.isEmpty()) {
            errors.put("roomId", String.join(" ", problems));
        }
        return rooms;
    }

    // Chặn phòng không tồn tại, dừng hoạt động hoặc đang bảo trì
    private String roomStatusError(Room room) {
        if (room == null) {
            return Messages.SHOWTIME_ROOM_INVALID;
        }
        if (RoomStatus.INACTIVE.equals(room.getStatus())) {
            return Messages.SHOWTIME_ROOM_INACTIVE;
        }
        if (RoomStatus.MAINTENANCE.equals(room.getStatus())) {
            return Messages.SHOWTIME_ROOM_MAINTENANCE;
        }
        if (!RoomStatus.ACTIVE.equals(room.getStatus())) {
            return Messages.SHOWTIME_ROOM_INVALID;
        }
        return null;
    }

    // Góp ý 5: phim không khai báo định dạng thì chiếu được mọi phòng; phòng không khai báo định dạng tính là 2D
    private String formatMismatch(Movie movie, Room room) {
        if (movie.getFormats() == null || movie.getFormats().isBlank()) {
            return null;
        }
        String roomFormat = roomFormatOf(room);
        for (String f : movie.getFormats().split("[,;/]")) {
            if (f.trim().equalsIgnoreCase(roomFormat)) {
                return null;
            }
        }
        return Messages.format(Messages.SHOWTIME_FORMAT_MISMATCH,
                movie.getTitle(), movie.getFormats(), room.getRoomName(), roomFormat);
    }

    private String roomFormatOf(Room room) {
        return room.getFormat() == null || room.getFormat().isBlank() ? "2D" : room.getFormat().trim();
    }

    // Giá vé tùy chọn: để trống trả về null (tự tính giá gợi ý); có nhập thì phải >= 10.000đ và là bội số của 1.000đ
    private BigDecimal parseOptionalPrice(String value, Map<String, String> errors) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value.trim());
            if (price.compareTo(new BigDecimal("10000")) >= 0
                    && price.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0) {
                return price;
            }
        } catch (NumberFormatException ignored) {
        }
        errors.put("basePrice", Messages.SHOWTIME_PRICE_INVALID);
        return null;
    }

    private String normalizeTranslationType(String translationType) {
        return "Lồng tiếng".equals(translationType) ? "Lồng tiếng" : "Phụ đề";
    }

    private Showtime newShowtime(int movieId, int roomId, LocalDateTime start, LocalDateTime end, BigDecimal price,
                                 String translationType) {
        Showtime showtime = new Showtime();
        showtime.setMovieId(movieId);
        showtime.setRoomId(roomId);
        showtime.setStartTime(start);
        showtime.setEndTime(end);
        showtime.setBasePrice(price);
        showtime.setStatus(ShowtimeStatus.ACTIVE);
        showtime.setTranslationType(translationType);
        return showtime;
    }

    /**
     * Hủy suất chiếu (Góp ý 2):
     * Nếu suất chiếu đã có vé bán cho khách hàng, CHẶN HỦY để bảo vệ quyền lợi và tiền vé của khách.
     */
    public void cancel(int showtimeId) throws SQLException, ValidationException {
        Showtime existing = showtimeDAO.findById(showtimeId);
        if (existing == null) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_NOT_FOUND));
        }
        if (ShowtimeStatus.CANCELLED.equals(existing.getStatus())) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_CANCEL_ALREADY));
        }
        if (showtimeDAO.hasSoldTickets(showtimeId)) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_CANCEL_HAS_SOLD_TICKETS));
        }
        showtimeDAO.cancel(showtimeId);
    }

    /**
     * Xóa suất chiếu khỏi DB (Góp ý 3):
     * Kiểm tra toàn diện mọi khóa ngoại (Ticket, Booking, Seat_Hold).
     * Nếu có dù chỉ 1 dòng tham chiếu thì CHẶN XÓA để tránh vi phạm Foreign Key Constraint (Lỗi 500).
     */
    public void delete(int showtimeId) throws SQLException, ValidationException {
        Showtime existing = showtimeDAO.findById(showtimeId);
        if (existing == null) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_NOT_FOUND));
        }
        if (showtimeDAO.hasAnyReferences(showtimeId)) {
            throw new ValidationException(Map.of("general", Messages.SHOWTIME_DELETE_HAS_REFERENCES));
        }
        try {
            showtimeDAO.delete(showtimeId);
        } catch (SQLException e) {
            if (e.getErrorCode() == 547) { // SQL Server Foreign Key Violation
                throw new ValidationException(Map.of("general", Messages.SHOWTIME_DELETE_HAS_REFERENCES));
            }
            throw e;
        }
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
