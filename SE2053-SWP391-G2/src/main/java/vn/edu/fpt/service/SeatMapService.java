package vn.edu.fpt.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vn.edu.fpt.dao.MovieDAO;
import vn.edu.fpt.dao.SeatMapDAO;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.MovieStatus;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.RoomStatus;
import vn.edu.fpt.model.Seat;
import vn.edu.fpt.model.SeatStatus;
import vn.edu.fpt.model.SeatType;
import vn.edu.fpt.model.Showtime;
import vn.edu.fpt.model.ShowtimeStatus;
import vn.edu.fpt.util.Messages;

// UC-09: View Real-Time Seat Map. BR-SEAT-02: chỉ đọc, không tạo hay thay đổi Seat_Hold
public class SeatMapService {
    // Chu kỳ trình duyệt tải lại trạng thái ghế
    public static final int REFRESH_SECONDS = 10;

    // GB-11: chỉ suất chiếu của phim đang công khai mới xem được
    private static final List<String> PUBLIC_MOVIE_STATUSES = List.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON);

    private final SeatMapDAO seatMapDAO = new SeatMapDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    public SeatMapView load(int showtimeId) throws SQLException, ValidationException {
        Showtime showtime = showtimeId > 0 ? seatMapDAO.findShowtime(showtimeId) : null;
        if (showtime == null || !ShowtimeStatus.ACTIVE.equals(showtime.getStatus())) {
            throw error(Messages.SEAT_MAP_SHOWTIME_NOT_FOUND);
        }
        Movie movie = movieDAO.findById(showtime.getMovieId());
        if (movie == null || !PUBLIC_MOVIE_STATUSES.contains(movie.getStatus())) {
            throw error(Messages.SEAT_MAP_SHOWTIME_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!showtime.getStartTime().isAfter(now)) {
            throw error(Messages.SEAT_MAP_SHOWTIME_STARTED);
        }
        Room room = seatMapDAO.findRoom(showtime.getRoomId());
        if (room == null) {
            throw error(Messages.SEAT_MAP_SHOWTIME_NOT_FOUND);
        }
        // E2: phòng bị Manager khóa bảo trì thì chặn xem sơ đồ
        if (RoomStatus.MAINTENANCE.equals(room.getStatus())) {
            throw error(Messages.SEAT_MAP_ROOM_MAINTENANCE);
        }
        List<Seat> seats = seatMapDAO.findSeatsByRoom(room.getRoomId());
        if (seats.isEmpty()) {
            throw error(Messages.SEAT_MAP_NO_SEATS);
        }

        Set<Integer> soldIds = seatMapDAO.findSoldSeatIds(showtimeId);
        Set<Integer> pendingIds = seatMapDAO.findPendingSeatIds(showtimeId);
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (String status : SeatStatus.ALL) {
            statusCounts.put(status, 0);
        }
        List<SeatMapCell> cells = new ArrayList<>();
        int columnCount = 0;
        for (Seat seat : seats) {
            String status = resolveStatus(seat, soldIds, pendingIds);
            statusCounts.merge(status, 1, Integer::sum);
            cells.add(new SeatMapCell(seat.getSeatId(), seat.getRowName(), seat.getColNumber(),
                    seat.getSeatType(), status));
            // Ghế đôi chiếm 2 cột (ColNumber là cột trái)
            columnCount = Math.max(columnCount, seat.getColNumber() + SeatType.widthOf(seat.getSeatType()) - 1);
        }
        return new SeatMapView(showtime, movie, room, buildRows(cells, columnCount), cells, columnCount,
                statusCounts, now);
    }

    // GB-02: mỗi ghế đúng một trạng thái; ghế hỏng ưu tiên cao nhất, sau đó đến đã bán, đang giữ
    private String resolveStatus(Seat seat, Set<Integer> soldIds, Set<Integer> pendingIds) {
        if (RoomStatus.MAINTENANCE.equals(seat.getStatus())) {
            return SeatStatus.MAINTENANCE;
        }
        if (soldIds.contains(seat.getSeatId())) {
            return SeatStatus.SOLD;
        }
        if (pendingIds.contains(seat.getSeatId())) {
            return SeatStatus.PENDING;
        }
        return SeatStatus.AVAILABLE;
    }

    // Đặt ghế vào lưới theo ColNumber để các hàng thẳng cột, chỗ không có ghế để trống (null).
    // Ghế đôi chiếm thêm cột bên phải nên cột đó không có phần tử riêng (JSP cho ghế đôi trải 2 cột).
    private List<SeatMapRow> buildRows(List<SeatMapCell> cells, int columnCount) {
        Map<String, SeatMapCell[]> byRow = new LinkedHashMap<>();
        for (SeatMapCell cell : cells) {
            if (cell.getColNumber() < 1) {
                continue;
            }
            byRow.computeIfAbsent(cell.getRowName(), k -> new SeatMapCell[columnCount])[cell.getColNumber() - 1] = cell;
        }
        List<SeatMapRow> rows = new ArrayList<>();
        for (Map.Entry<String, SeatMapCell[]> entry : byRow.entrySet()) {
            SeatMapCell[] line = entry.getValue();
            List<SeatMapCell> rowCells = new ArrayList<>();
            for (int col = 0; col < line.length; col++) {
                rowCells.add(line[col]);
                if (line[col] != null) {
                    col += SeatType.widthOf(line[col].getSeatType()) - 1;
                }
            }
            rows.add(new SeatMapRow(entry.getKey(), rowCells));
        }
        return rows;
    }

    private ValidationException error(String message) {
        return new ValidationException(Map.of("general", message));
    }
}
