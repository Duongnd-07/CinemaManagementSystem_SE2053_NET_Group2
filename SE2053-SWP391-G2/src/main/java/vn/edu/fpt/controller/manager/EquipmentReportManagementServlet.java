package vn.edu.fpt.controller.manager;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.EquipmentReport;
import vn.edu.fpt.model.ReportStatus;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.EquipmentReportService;
import vn.edu.fpt.service.EquipmentReportUpdateForm;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-26: Process Equipment Reports
@WebServlet("/manager/equipment-reports")
public class EquipmentReportManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(EquipmentReportManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/equipment-reports.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String ITEM_LABEL = "báo cáo sự cố";

    private final EquipmentReportService reportService = new EquipmentReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String idParam = trim(request.getParameter("id"));
            if (idParam.isEmpty()) {
                render(request, response, null, null, new LinkedHashMap<>());
                return;
            }
            EquipmentReport report = reportService.findById(parseInt(idParam));
            if (report == null) {
                redirectWithError(request, response, Messages.REPORT_NOT_FOUND);
                return;
            }
            EquipmentReportUpdateForm form = new EquipmentReportUpdateForm();
            form.setNewStatus(report.getStatus());
            form.setLockRoom(reportService.isRoomLocked(report));
            render(request, response, report, form, new LinkedHashMap<>());
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
        try {
            EquipmentReport report = reportService.findById(parseInt(request.getParameter("reportId")));
            if (report == null) {
                redirectWithError(request, response, Messages.REPORT_NOT_FOUND);
                return;
            }
            EquipmentReportUpdateForm form = readForm(request);
            EquipmentReportService.UpdateResult result;
            try {
                result = reportService.process(report, form, currentUserId(request));
            } catch (ValidationException e) {
                // Nạp lại vì trạng thái có thể vừa bị người khác đổi
                render(request, response, reportService.findById(report.getReportId()), form, e.getErrors());
                return;
            }
            request.getSession().setAttribute(FLASH_SUCCESS, successMessage(report, result));
            response.sendRedirect(listUrl(request));
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, EquipmentReport report,
                        EquipmentReportUpdateForm form, Map<String, String> errors)
            throws SQLException, ServletException, IOException {
        String status = trim(request.getParameter("status"));

        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("reportPage", reportService.listAll(status, parseInt(request.getParameter("page"))));
        request.setAttribute("statusCounts", reportService.countAllByStatus());
        request.setAttribute("statusFilter", ReportStatus.isValid(status) ? status : "");
        request.setAttribute("statuses", ReportStatus.ALL);
        request.setAttribute("listQuery", listQuery(request));
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "equipment-reports");
        request.setAttribute("msgNoResult", Messages.MSG01);
        if (report != null) {
            request.setAttribute("report", report);
            request.setAttribute("form", form);
            request.setAttribute("errors", errors);
            request.setAttribute("logs", reportService.listLogs(report.getReportId()));
            request.setAttribute("allowedStatuses", reportService.allowedStatuses(report.getStatus()));
            request.setAttribute("resolved", ReportStatus.RESOLVED.equals(report.getStatus()));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private EquipmentReportUpdateForm readForm(HttpServletRequest request) {
        EquipmentReportUpdateForm form = new EquipmentReportUpdateForm();
        form.setNewStatus(trim(request.getParameter("newStatus")));
        form.setNote(trim(request.getParameter("note")));
        form.setLockRoom("1".equals(request.getParameter("lockRoom")));
        return form;
    }

    private String successMessage(EquipmentReport report, EquipmentReportService.UpdateResult result) {
        StringBuilder message = new StringBuilder(Messages.format(Messages.MSG03, ITEM_LABEL));
        if (result.isSeatReopened()) {
            message.append(' ').append(Messages.format(Messages.REPORT_SEAT_REOPENED, report.getSeatLabel()));
        }
        if (result.isRoomLocked()) {
            message.append(' ').append(result.getUpcomingShowtimes() > 0
                    ? Messages.format(Messages.REPORT_ROOM_LOCKED_SHOWTIMES, report.getRoomName(),
                            result.getUpcomingShowtimes())
                    : Messages.format(Messages.REPORT_ROOM_LOCKED, report.getRoomName()));
        } else if (result.isRoomUnlocked()) {
            message.append(' ').append(Messages.format(Messages.REPORT_ROOM_UNLOCKED, report.getRoomName()));
        }
        return message.toString();
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        request.getSession().setAttribute(FLASH_ERROR, message);
        response.sendRedirect(listUrl(request));
    }

    private String listUrl(HttpServletRequest request) {
        return request.getContextPath() + "/manager/equipment-reports" + listQuery(request);
    }

    // Người xử lý luôn là người đang đăng nhập, không nhận từ form
    private int currentUserId(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        return user.getUserId();
    }

    // Giữ bộ lọc/trang hiện tại khi mở và đóng hộp thoại
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
