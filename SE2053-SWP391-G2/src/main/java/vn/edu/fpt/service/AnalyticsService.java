package vn.edu.fpt.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vn.edu.fpt.dao.AnalyticsDAO;
import vn.edu.fpt.model.OccupancyStat;
import vn.edu.fpt.model.SalesSummary;

// UC-29: View Analytics Dashboard
public class AnalyticsService {
    public static final String DAY = "day";
    public static final String WEEK = "week";
    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final List<String> PERIODS = List.of(DAY, WEEK, MONTH, YEAR);

    public static final String TIER_TOP = "Top";
    public static final String TIER_AVERAGE = "Average";
    public static final String TIER_LOW = "Low";

    private static final String CONFIG_TOP = "TOP_MOVIE_OCCUPANCY_PERCENT";
    private static final String CONFIG_LOW = "LOW_MOVIE_OCCUPANCY_PERCENT";
    private static final int DEFAULT_TOP = 70;
    private static final int DEFAULT_LOW = 30;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final AnalyticsDAO analyticsDAO = new AnalyticsDAO();

    public String normalizePeriod(String period) {
        return period != null && PERIODS.contains(period) ? period : MONTH;
    }

    // Ngày đầu kỳ chứa ngày anchor (tuần bắt đầu từ thứ Hai)
    public LocalDate periodStart(String period, LocalDate anchor) {
        switch (period) {
            case DAY:
                return anchor;
            case WEEK:
                return anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case YEAR:
                return anchor.withDayOfYear(1);
            default:
                return anchor.withDayOfMonth(1);
        }
    }

    // Ngày kết thúc kỳ, không tính ngày này
    public LocalDate periodEnd(String period, LocalDate start) {
        switch (period) {
            case DAY:
                return start.plusDays(1);
            case WEEK:
                return start.plusWeeks(1);
            case YEAR:
                return start.plusYears(1);
            default:
                return start.plusMonths(1);
        }
    }

    public SalesSummary summarize(LocalDate start, LocalDate end) throws SQLException {
        return analyticsDAO.summarize(start.atStartOfDay(), end.atStartOfDay());
    }

    // Doanh thu theo từng ngày của kỳ (kỳ năm thì theo tháng); mốc không có đơn vẫn xuất hiện với giá trị 0
    public Map<String, BigDecimal> revenueSeries(String period, LocalDate start, LocalDate end) throws SQLException {
        boolean byMonth = YEAR.equals(period);
        Map<LocalDate, BigDecimal> revenue = analyticsDAO.revenueByDate(start.atStartOfDay(), end.atStartOfDay(),
                byMonth);
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        for (LocalDate date = start; date.isBefore(end); date = byMonth ? date.plusMonths(1) : date.plusDays(1)) {
            series.put(date.format(byMonth ? MONTH_LABEL : DAY_LABEL), revenue.getOrDefault(date, BigDecimal.ZERO));
        }
        return series;
    }

    public List<OccupancyStat> occupancyByRoom(LocalDate start, LocalDate end) throws SQLException {
        return analyticsDAO.occupancyByRoom(start.atStartOfDay(), end.atStartOfDay());
    }

    // BR-M-02: xếp hạng phim theo tỷ lệ lấp đầy so với hai ngưỡng cấu hình trong SystemConfig
    public List<OccupancyStat> moviePerformance(LocalDate start, LocalDate end) throws SQLException {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atStartOfDay();
        int top = topThreshold();
        int low = lowThreshold();
        List<OccupancyStat> movies = analyticsDAO.occupancyByMovie(from, to);
        for (OccupancyStat movie : movies) {
            movie.setTier(tierOf(movie.getOccupancyPercent(), top, low));
        }
        return movies;
    }

    public int topThreshold() throws SQLException {
        return analyticsDAO.configInt(CONFIG_TOP, DEFAULT_TOP);
    }

    public int lowThreshold() throws SQLException {
        return analyticsDAO.configInt(CONFIG_LOW, DEFAULT_LOW);
    }

    static String tierOf(double occupancyPercent, int top, int low) {
        if (occupancyPercent >= top) {
            return TIER_TOP;
        }
        return occupancyPercent < low ? TIER_LOW : TIER_AVERAGE;
    }
}
