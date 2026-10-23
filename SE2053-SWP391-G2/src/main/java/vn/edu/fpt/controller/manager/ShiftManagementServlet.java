package vn.edu.fpt.controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ShiftReport;
import vn.edu.fpt.service.ShiftService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-22: View Shift Report (Manager - bàn giao ca). GET: danh sách ca theo ngày; ?id -> báo cáo một ca.
// POST action: forceClose (kết hộ ca nhân viên quên kết), count (nhập số đếm két cho ca chưa đếm)
@WebServlet("/manager/shifts")
public class ShiftManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ShiftManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/shifts.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String SHIFT_CLOSED = "Đã kết ca hộ nhân viên. Két chưa đếm, cần nhập số đếm khi bàn giao.";
    private static final String COUNT_SAVED = "Đã lưu số tiền đếm két.";
    private static final String SHIFT_NOT_FOUND = "Không tìm thấy ca làm việc.";

    private final ShiftService shiftService = new ShiftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            render(request, response, parseInt(request.getParameter("id")), new LinkedHashMap<>());
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int shiftId = parseInt(request.getParameter("shiftId"));
        try {
            try {
                if ("forceClose".equals(request.getParameter("action"))) {
                    shiftService.closeByManager(shiftId, currentUserId(request));
                    flash(request, FLASH_SUCCESS, SHIFT_CLOSED);
                } else {
                    shiftService.recordCount(shiftId, currentUserId(request), true,
                            request.getParameter("countedCash"), request.getParameter("cashNote"));
                    flash(request, FLASH_SUCCESS, COUNT_SAVED);
                }
                response.sendRedirect(request.getContextPath() + "/manager/shifts?id=" + shiftId);
            } catch (ValidationException e) {
                render(request, response, shiftId, e.getErrors());
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, int shiftId,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        HttpSession session = request.getSession();
        if (shiftId > 0) {
            ShiftReport report = shiftService.report(shiftId);
            if (report == null) {
                flash(request, FLASH_ERROR, SHIFT_NOT_FOUND);
                response.sendRedirect(request.getContextPath() + "/manager/shifts");
                return;
            }
            request.setAttribute("report", report);
            request.setAttribute("canForceClose", !report.isClosed());
        } else {
            LocalDate date = parseDate(request.getParameter("date"));
            request.setAttribute("date", date);
            request.setAttribute("shifts", shiftService.listByDate(date));
        }
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("countedCash", trim(request.getParameter("countedCash")));
        request.setAttribute("cashNote", trim(request.getParameter("cashNote")));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "shifts");
        request.setAttribute("formAction", request.getContextPath() + "/manager/shifts");
        request.setAttribute("canClose", false);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    // Ngày không hợp lệ hoặc bỏ trống thì lấy hôm nay
    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu báo cáo ca", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
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
