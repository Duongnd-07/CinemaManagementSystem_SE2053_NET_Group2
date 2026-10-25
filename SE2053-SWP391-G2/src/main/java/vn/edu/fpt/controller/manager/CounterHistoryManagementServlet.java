package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.CounterHistoryService;
import vn.edu.fpt.util.Messages;

// UC-19: Manager xem lịch sử bán tại quầy của mọi nhân viên theo ngày. GET ?date, ?q (mã đơn / mã vé / khách), ?page
@WebServlet("/manager/counter-history")
public class CounterHistoryManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CounterHistoryManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/counter-history.jsp";

    private final CounterHistoryService historyService = new CounterHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate date = parseDate(request.getParameter("date"));
        String keyword = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
        try {
            request.setAttribute("result", historyService.listForManager(date, keyword,
                    parseInt(request.getParameter("page"))));
            request.setAttribute("date", date);
            request.setAttribute("q", keyword);
            request.setAttribute("baseUrl", request.getContextPath() + "/manager/counter-history?date=" + date
                    + "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            request.setAttribute("activeMenu", "counter-history");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu lịch sử bán tại quầy", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    // Ngày không hợp lệ hoặc bỏ trống thì lấy hôm nay
    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
