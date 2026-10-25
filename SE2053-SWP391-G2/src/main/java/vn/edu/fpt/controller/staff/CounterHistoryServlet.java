package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.CounterHistoryService;
import vn.edu.fpt.util.Messages;

// UC-19: lịch sử bán vé tại quầy của nhân viên. GET ?shiftId (ca của mình, mặc định ca đang mở / gần nhất), ?q, ?page
@WebServlet("/staff/counter-history")
public class CounterHistoryServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CounterHistoryServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/counter-history.jsp";
    private static final String STAFF_ONLY = "Chỉ nhân viên bán vé mới xem được lịch sử bán theo ca.";

    private final CounterHistoryService historyService = new CounterHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        // AuthFilter cho cả Manager vào /staff/*; Manager xem ở /manager/counter-history
        if (user == null || !Role.STAFF.equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
            return;
        }
        String keyword = trim(request.getParameter("q"));
        try {
            List<Shift> shifts = historyService.listShifts(user.getUserId());
            Shift shift = historyService.resolveShift(shifts, parseInt(request.getParameter("shiftId")));
            if (shift != null) {
                request.setAttribute("result", historyService.listForStaff(user.getUserId(), shift.getShiftId(),
                        keyword, parseInt(request.getParameter("page"))));
                request.setAttribute("baseUrl", request.getContextPath() + "/staff/counter-history?shiftId="
                        + shift.getShiftId() + "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            }
            request.setAttribute("shifts", shifts);
            request.setAttribute("shift", shift);
            request.setAttribute("q", keyword);
            request.setAttribute("activeMenu", "counter-history");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu lịch sử bán tại quầy", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
