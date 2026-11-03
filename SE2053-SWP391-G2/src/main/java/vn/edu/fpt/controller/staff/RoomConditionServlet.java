package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.RoomCheckItem;
import vn.edu.fpt.model.RoomConditionReport;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.RoomConditionForm;
import vn.edu.fpt.service.RoomConditionService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-35: Report Room Condition Before Shift
@WebServlet("/staff/room-conditions")
public class RoomConditionServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RoomConditionServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/room-conditions.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String STAFF_ONLY = "Chỉ nhân viên mới được báo cáo tình trạng phòng.";

    private final RoomConditionService conditionService = new RoomConditionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        try {
            String roomId = trim(request.getParameter("roomId"));
            RoomConditionForm form = null;
            if (!roomId.isEmpty()) {
                form = new RoomConditionForm();
                form.setRoomId(roomId);
            }
            render(request, response, form, Map.of());
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
        try {
            RoomConditionForm form = readForm(request);
            try {
                RoomConditionReport report = conditionService.submit(form, currentUser(request).getUserId());
                ActivityLogger.info(request, LogEventType.CATALOG, "Báo cáo tình trạng phòng", "RoomCondition",
                        report.getReportId(), report.getRoomName() + ": " + (report.isIssue() ? "Có vấn đề" : "Đạt"));
                request.getSession().setAttribute(FLASH_SUCCESS, report.isIssue()
                        ? Messages.ROOM_CONDITION_SAVED_ISSUE : Messages.ROOM_CONDITION_SAVED_GOOD);
                response.sendRedirect(request.getContextPath() + "/staff/room-conditions");
            } catch (ValidationException e) {
                render(request, response, form, e.getErrors());
            }
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, RoomConditionForm form,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        int staffId = currentUser(request).getUserId();
        boolean inShift = conditionService.isInShift(staffId);
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("rooms", conditionService.listRooms());
        request.setAttribute("latestToday", conditionService.latestToday());
        request.setAttribute("inShift", inShift);
        request.setAttribute("reportPage", conditionService.listByStaff(staffId, parseInt(request.getParameter("page"))));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "room-conditions");
        request.setAttribute("msgNoResult", Messages.MSG01);
        // Đang trong ca thì không mở form; lỗi từ POST vẫn phải hiện để nhân viên biết vì sao chưa lưu
        if (form != null && (!inShift || !errors.isEmpty())) {
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.setAttribute("showForm", true);
            request.setAttribute("checkItems", RoomCheckItem.values());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private RoomConditionForm readForm(HttpServletRequest request) {
        RoomConditionForm form = new RoomConditionForm();
        form.setRoomId(trim(request.getParameter("roomId")));
        form.setNote(trim(request.getParameter("note")));
        for (RoomCheckItem item : RoomCheckItem.values()) {
            form.getResults().put(item.getKey(), trim(request.getParameter("result_" + item.getKey())));
            form.getItemNotes().put(item.getKey(), trim(request.getParameter("note_" + item.getKey())));
        }
        return form;
    }

    // AuthFilter cho Manager vào /staff/*, nhưng kiểm tra phòng trước ca là việc của nhân viên
    private boolean isStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = currentUser(request);
        if (user != null && Role.STAFF.equals(user.getRole())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
        return false;
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu báo cáo tình trạng phòng", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
