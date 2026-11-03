package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.ShiftReport;
import vn.edu.fpt.service.ShiftService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-22: View Shift Report (nhân viên). GET: báo cáo ca đang mở + lịch sử ca; ?id -> báo cáo một ca của mình.
// POST action: close (kết ca kèm đếm két), count (nhập số đếm két cho ca đã tự kết / Manager kết hộ)
@WebServlet("/staff/shifts")
public class StaffShiftServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StaffShiftServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/shifts.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String STAFF_ONLY = "Chỉ nhân viên quầy mới có ca làm việc.";
    private static final String SHIFT_CLOSED = "Đã kết ca. Báo cáo ca đã được chốt.";
    private static final String COUNT_SAVED = "Đã lưu số tiền đếm két.";
    private static final String SHIFT_NOT_FOUND = "Không tìm thấy ca làm việc.";

    private final ShiftService shiftService = new ShiftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        try {
            render(request, response, parseInt(request.getParameter("id")), new LinkedHashMap<>());
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        int staffId = currentUserId(request);
        String countedCash = request.getParameter("countedCash");
        String cashNote = request.getParameter("cashNote");
        try {
            if ("close".equals(request.getParameter("action"))) {
                try {
                    int shiftId = shiftService.closeByStaff(staffId, countedCash, cashNote);
                    ActivityLogger.info(request, LogEventType.COUNTER, "Đóng ca bán hàng", "Shift", shiftId,
                            "Tiền đếm được " + countedCash + (cashNote == null || cashNote.isBlank() ? ""
                                    : ", ghi chú: " + cashNote.trim()));
                    flash(request, FLASH_SUCCESS, SHIFT_CLOSED);
                    redirect(request, response, shiftId);
                } catch (ValidationException e) {
                    render(request, response, 0, e.getErrors());
                }
                return;
            }
            int shiftId = parseInt(request.getParameter("shiftId"));
            try {
                shiftService.recordCount(shiftId, staffId, false, countedCash, cashNote);
                ActivityLogger.info(request, LogEventType.COUNTER, "Nhập tiền đếm cuối ca", "Shift", shiftId,
                        "Tiền đếm được " + countedCash + (cashNote == null || cashNote.isBlank() ? ""
                                : ", ghi chú: " + cashNote.trim()));
                flash(request, FLASH_SUCCESS, COUNT_SAVED);
                redirect(request, response, shiftId);
            } catch (ValidationException e) {
                render(request, response, shiftId, e.getErrors());
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    // shiftId = 0: trang tổng quan (ca đang mở + lịch sử); ngược lại báo cáo của một ca
    private void render(HttpServletRequest request, HttpServletResponse response, int shiftId,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        int staffId = currentUserId(request);
        ShiftReport report;
        if (shiftId > 0) {
            report = shiftService.report(shiftId);
            // Nhân viên chỉ xem được ca của chính mình
            if (report == null || report.getShift().getStaffId() != staffId) {
                flash(request, FLASH_ERROR, SHIFT_NOT_FOUND);
                response.sendRedirect(request.getContextPath() + "/staff/shifts");
                return;
            }
        } else {
            Shift open = shiftService.findOpenShift(staffId);
            report = open == null ? null : shiftService.report(open.getShiftId());
            request.setAttribute("history", shiftService.listMine(staffId));
        }
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("report", report);
        request.setAttribute("errors", errors);
        request.setAttribute("countedCash", trim(request.getParameter("countedCash")));
        request.setAttribute("cashNote", trim(request.getParameter("cashNote")));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "shifts");
        request.setAttribute("formAction", request.getContextPath() + "/staff/shifts");
        request.setAttribute("canClose", report != null && !report.isClosed());
        request.setAttribute("receiptBase", request.getContextPath() + "/staff/counter?receipt=");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // AuthFilter cho cả Manager vào /staff/*, nhưng ca làm việc là của nhân viên quầy (UC-19)
    private boolean isStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        if (user != null && Role.STAFF.equals(user.getRole())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
        return false;
    }

    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, int shiftId) throws IOException {
        response.sendRedirect(request.getContextPath() + "/staff/shifts?id=" + shiftId);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu ca làm việc", e);
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
