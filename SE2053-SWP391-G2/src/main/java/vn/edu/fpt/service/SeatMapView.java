package vn.edu.fpt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.model.Movie;
import vn.edu.fpt.model.Room;
import vn.edu.fpt.model.SeatStatus;
import vn.edu.fpt.model.Showtime;

// UC-09: dữ liệu sơ đồ ghế của một suất chiếu tại một thời điểm
public class SeatMapView {
    private final Showtime showtime;
    private final Movie movie;
    private final Room room;
    private final List<SeatMapRow> rows;
    private final List<SeatMapCell> seats;
    private final int columnCount;
    private final Map<String, Integer> statusCounts;
    private final LocalDateTime loadedAt;

    public SeatMapView(Showtime showtime, Movie movie, Room room, List<SeatMapRow> rows, List<SeatMapCell> seats,
                       int columnCount, Map<String, Integer> statusCounts, LocalDateTime loadedAt) {
        this.showtime = showtime;
        this.movie = movie;
        this.room = room;
        this.rows = rows;
        this.seats = seats;
        this.columnCount = columnCount;
        this.statusCounts = statusCounts;
        this.loadedAt = loadedAt;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public Movie getMovie() {
        return movie;
    }

    public Room getRoom() {
        return room;
    }

    public List<SeatMapRow> getRows() {
        return rows;
    }

    public List<SeatMapCell> getSeats() {
        return seats;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    // E1: không còn ghế nào AVAILABLE (đều đã bán, đang giữ hoặc bảo trì)
    public boolean isFullyBooked() {
        return !seats.isEmpty() && statusCounts.getOrDefault(SeatStatus.AVAILABLE, 0) == 0;
    }
}
