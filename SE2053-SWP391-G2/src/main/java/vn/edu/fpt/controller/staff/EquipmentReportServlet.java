package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.EquipmentCategory;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.UrgencyLevel;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.EquipmentReportForm;
import vn.edu.fpt.service.EquipmentReportService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-21: Report Equipment Issue
@WebServlet("/staff/equipment-reports")
public class EquipmentReportServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(EquipmentReportServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/equipment-reports.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String ITEM_LABEL = "báo cáo sự cố";
    private static final String STAFF_ONLY = "Chỉ nhân viên mới được gửi báo cáo sự cố.";
    private static final String ROOM_AUTO_LOCKED =
            "Phòng chiếu đã tự động khóa bảo trì vì có từ 2 thiết bị hỏng mức Cao/Khẩn cấp.";
    private static final String ROOM_AUTO_LOCKED_SHOWTIMES = " Phòng đang có {0} suất chiếu sắp tới, cần báo quản lý.";

    private final EquipmentReportService reportService = new EquipmentReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isStaff(request, response)) {
            return;
        }
        try {
            EquipmentReportForm form = "1".equals(request.getParameter("add")) ? new EquipmentReportForm() : null;
            render(request, response, form, new LinkedHashMap<>());
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
            EquipmentReportForm form = readForm(request);
            EquipmentReportService.SubmitResult result;
            try {
                result = reportService.submit(form, currentUserId(request));
            } catch (ValidationException e) {
                render(request, response, form, e.getErrors());
                return;
            }
            request.getSession().setAttribute(FLASH_SUCCESS, successMessage(form, result));
            response.sendRedirect(request.getContextPath() + "/staff/equipment-reports");
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, EquipmentReportForm form,
                        Map<String, String> errors) throws SQLException, ServletException, IOException {
        int staffId = currentUserId(request);
        String status = trim(request.getParameter("status"));

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("reportPage",
                reportService.listByStaff(staffId, status, parseInt(request.getParameter("page"))));
        request.setAttribute("statusCounts", reportService.countByStatus(staffId));
        request.setAttribute("statusFilter", ReportStatus.isValid(status) ? status : "");
        request.setAttribute("statuses", ReportStatus.ALL);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "equipment-reports");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (form != null) {
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.setAttribute("showForm", true);
            request.setAttribute("categories", EquipmentCategory.ALL);
            request.setAttribute("urgencyLevels", UrgencyLevel.ALL);
            request.setAttribute("rooms", reportService.listRooms());
            request.setAttribute("seats", reportService.listSeats());
            request.setAttribute("equipment", reportService.listEquipment());
            request.setAttribute("openReports", reportService.listOpenReports());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private EquipmentReportForm readForm(HttpServletRequest request) {
        EquipmentReportForm form = new EquipmentReportForm();
        form.setCategory(trim(request.getParameter("category")));
        form.setRoomId(trim(request.getParameter("roomId")));
        form.setSeatIds(values(request, "seatIds"));
        form.setEquipmentIds(values(request, "equipmentIds"));
        form.setUrgency(trim(request.getParameter("urgency")));
        form.setDescription(trim(request.getParameter("description")));
        return form;
    }

    // Checkbox chọn nhiều ghế / thiết bị gửi lên nhiều giá trị cùng tên
    private List<String> values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(values));
    }

    private String successMessage(EquipmentReportForm form, EquipmentReportService.SubmitResult result) {
        String label = result.getReportCount() > 1 ? result.getReportCount() + " " + ITEM_LABEL : ITEM_LABEL;
        String message = Messages.format(Messages.MSG04, label);
        if (result.isRoomLocked()) {
            message += " " + ROOM_AUTO_LOCKED + (result.getUpcomingShowtimes() > 0
                    ? Messages.format(ROOM_AUTO_LOCKED_SHOWTIMES, result.getUpcomingShowtimes()) : "");
        }
        if (!EquipmentCategory.SEAT.equals(form.getCategory())) {
            return message;
        }
        return message + " " + (result.getAffectedTickets() > 0
                ? Messages.format(Messages.REPORT_SEAT_HAS_TICKETS, result.getAffectedTickets())
                : Messages.REPORT_SEAT_LOCKED);
    }

    // AuthFilter cho Manager/Admin vào /staff/*, nhưng chỉ nhân viên tác nghiệp mới được báo cáo sự cố
    private boolean isStaff(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        if (user != null && Role.STAFF.equals(user.getRole())) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, STAFF_ONLY);
        return false;
    }

    // Người gửi báo cáo luôn là người đang đăng nhập, không nhận từ form
    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    // Giữ bộ lọc/trang hiện tại khi mở và đóng form
    private String listQuery(HttpServletRequest request) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("status", "page")) {
            String value = trim(request.getParameter(name));
            if (!value.isEmpty()) {
                parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu báo cáo sự cố", e);
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
