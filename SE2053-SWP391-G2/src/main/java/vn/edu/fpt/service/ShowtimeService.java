package vn.edu.fpt.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import vn.edu.fpt.dao.ShowtimeDAO;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.Showtime;

public class ShowtimeService {
    // UC-08: mặc định xem hôm nay và 7 ngày tới
    public static final int SCHEDULE_DAYS_AHEAD = 7;
    // Nhóm cho phòng chưa khai báo định dạng (Room.Format cho phép NULL)
    private static final String OTHER_FORMAT = "Khác";

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

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
