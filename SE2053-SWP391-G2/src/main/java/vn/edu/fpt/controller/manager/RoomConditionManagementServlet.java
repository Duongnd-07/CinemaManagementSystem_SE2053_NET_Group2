package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.RoomConditionService;
import vn.edu.fpt.util.Messages;

// UC-35: Manager xem báo cáo tình trạng phòng theo ngày. Báo cáo chỉ đọc; xử lý hỏng hóc đi qua UC-21/UC-26.
@WebServlet("/manager/room-conditions")
public class RoomConditionManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomConditionManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/room-conditions.jsp";

    private final RoomConditionService conditionService = new RoomConditionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate date = parseDate(request.getParameter("date"));
        String result = request.getParameter("result");
        try {
            request.setAttribute("reportPage", conditionService.listByDate(date, result, parseInt(request.getParameter("page"))));
            if (date.equals(LocalDate.now())) {
                request.setAttribute("uncheckedToday", conditionService.countUncheckedToday());
            }
            request.setAttribute("date", date);
            request.setAttribute("resultFilter", result == null ? "" : result);
            request.setAttribute("activeMenu", "room-conditions");
            request.setAttribute("msgNoResult", Messages.MSG01);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu báo cáo tình trạng phòng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
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
