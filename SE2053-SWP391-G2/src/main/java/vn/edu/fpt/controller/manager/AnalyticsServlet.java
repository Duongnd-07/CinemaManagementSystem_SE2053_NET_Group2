package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.OccupancyStat;
import vn.edu.fpt.service.AnalyticsService;
import vn.edu.fpt.util.Messages;

// UC-29: View Analytics Dashboard. AuthFilter chỉ cho Manager/Admin vào /manager/*
@WebServlet("/manager/reports")
public class AnalyticsServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AnalyticsServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/analytics.jsp";
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final AnalyticsService analyticsService = new AnalyticsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String period = analyticsService.normalizePeriod(request.getParameter("period"));
        LocalDate anchor = parseDate(request.getParameter("date"));
        LocalDate start = analyticsService.periodStart(period, anchor);
        LocalDate end = analyticsService.periodEnd(period, start);
        try {
            Map<String, BigDecimal> revenueSeries = analyticsService.revenueSeries(period, start, end);
            List<OccupancyStat> rooms = analyticsService.occupancyByRoom(start, end);

            request.setAttribute("period", period);
            request.setAttribute("periods", AnalyticsService.PERIODS);
            request.setAttribute("anchor", anchor);
            request.setAttribute("periodLabel", start.format(DISPLAY_DATE) + " - "
                    + end.minusDays(1).format(DISPLAY_DATE));
            request.setAttribute("summary", analyticsService.summarize(start, end));
            request.setAttribute("revenueSeries", revenueSeries);
            request.setAttribute("revenueMax", Collections.max(revenueSeries.values()));
            request.setAttribute("rooms", rooms);
            request.setAttribute("movies", analyticsService.moviePerformance(start, end));
            request.setAttribute("topThreshold", analyticsService.topThreshold());
            request.setAttribute("lowThreshold", analyticsService.lowThreshold());
            request.setAttribute("activeMenu", "reports");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu thống kê", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    // Ngày không hợp lệ, bỏ trống hoặc ngoài khoảng năm hợp lý thì lấy hôm nay
    private LocalDate parseDate(String value) {
        try {
            LocalDate date = value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
            return date.getYear() < MIN_YEAR || date.getYear() > MAX_YEAR ? LocalDate.now() : date;
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }
}
