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
import vn.edu.fpt.model.Announcement;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.NotificationCategory;
import vn.edu.fpt.model.NotificationTarget;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.AnnouncementForm;
import vn.edu.fpt.service.AnnouncementService;
import vn.edu.fpt.service.CustomerNotificationService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

@WebServlet("/manager/announcements")
public class NotificationManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(NotificationManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/manager/announcements.jsp";
    private static final String PATH = "/manager/announcements";
    private final AnnouncementService announcementService = new AnnouncementService();
    private final CustomerNotificationService notificationService = new CustomerNotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            AnnouncementForm form = new AnnouncementForm();
            form.setTarget(NotificationTarget.ALL);
            form.setType(NotificationCategory.GENERAL);
            request.setAttribute("form", form);
            render(request, response);
        } catch (SQLException e) {
            systemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String action = request.getParameter("action");
        if (!List.of("preview", "send", "recall").contains(action == null ? "" : action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        HttpSession session = request.getSession(false);
        User manager = session == null ? null : (User) session.getAttribute("user");
        if (manager == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            if ("recall".equals(action)) {
                try {
                    Announcement announcement = announcementService.recallAnnouncement(parseInt(request.getParameter("id")), manager);
                    log(request, Messages.ANNOUNCEMENT_RECALL_ACTION, announcement);
                    session.setAttribute("flashSuccess", Messages.ANNOUNCEMENT_RECALLED);
                } catch (ValidationException e) {
                    session.setAttribute("flashError", e.getErrors().values().iterator().next());
                }
                response.sendRedirect(request.getContextPath() + PATH + listQuery(request, true));
                return;
            }
            AnnouncementForm form = readForm(request);
            request.setAttribute("form", form);
            try {
                if ("preview".equals(action)) {
                    request.setAttribute("preview", announcementService.preview(form, manager));
                } else {
                    Announcement announcement = announcementService.sendAnnouncement(form, manager);
                    log(request, Messages.ANNOUNCEMENT_SEND_ACTION, announcement);
                    session.setAttribute("flashSuccess", Messages.format(Messages.ANNOUNCEMENT_SENT, announcement.getRecipientCount()));
                    response.sendRedirect(request.getContextPath() + PATH);
                    return;
                }
            } catch (ValidationException e) {
                request.setAttribute("errors", e.getErrors());
            }
            render(request, response);
        } catch (SQLException e) {
            systemError(response, e);
        }
    }

    private void log(HttpServletRequest request, String action, Announcement announcement) {
        ActivityLogger.info(request, LogEventType.NOTIFICATION, action, "Announcement", announcement.getAnnouncementId(),
                Messages.format(Messages.ANNOUNCEMENT_LOG_DESCRIPTION,
                        NotificationTarget.labelOf(announcement.getTargetType()),
                        NotificationCategory.labelOf(announcement.getNotificationType()), announcement.getRecipientCount()));
    }

    private void render(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        // The compose form and history filters share names; on POST history uses separate hidden filter fields.
        String target = filterValue(request, "target");
        String type = filterValue(request, "type");
        String keyword = filterValue(request, "keyword");
        String page = filterValue(request, "page");
        if (!NotificationTarget.isValid(target)) target = "";
        if (!NotificationCategory.VALUES.contains(type)) type = "";
        request.setAttribute("history", announcementService.search(target, type, keyword, parseInt(page)));
        request.setAttribute("targetFilter", target);
        request.setAttribute("typeFilter", type);
        request.setAttribute("keyword", keyword);
        Map<String, String> targets = new LinkedHashMap<>();
        Map<String, List<String>> categories = new LinkedHashMap<>();
        Map<String, String> categoryLabels = new LinkedHashMap<>();
        for (String value : NotificationTarget.VALUES) {
            targets.put(value, NotificationTarget.labelOf(value));
            categories.put(value, NotificationCategory.selectableFor(value));
        }
        for (String value : NotificationCategory.VALUES) categoryLabels.put(value, NotificationCategory.labelOf(value));
        request.setAttribute("targets", targets);
        request.setAttribute("categories", categories);
        request.setAttribute("categoryLabels", categoryLabels);
        request.setAttribute("historyUrl", request.getContextPath() + PATH + listQuery(request, false));
        HttpSession session = request.getSession();
        // Preview/validation forwards are POSTs, so the GET-only badge filter does not run for them.
        if ("POST".equals(request.getMethod())) {
            User manager = (User) session.getAttribute("user");
            if (manager != null) {
                try {
                    request.setAttribute("unreadNotificationCount", notificationService.countUnread(manager.getUserId()));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Không đếm được thông báo chưa đọc khi xem trước", e);
                }
            }
        }
        for (String key : List.of("flashSuccess", "flashError")) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.setAttribute("activeMenu", "announcements");
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String filterValue(HttpServletRequest request, String name) {
        return trim(request.getParameter("POST".equals(request.getMethod()) ? "filter_" + name : name));
    }

    private String listQuery(HttpServletRequest request, boolean includePage) {
        List<String> parts = new ArrayList<>();
        for (String name : List.of("target", "type", "keyword", "page")) {
            if (!includePage && "page".equals(name)) continue;
            String value = filterValue(request, name);
            if (!value.isEmpty()) parts.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return parts.isEmpty() ? "" : "?" + String.join("&", parts);
    }

    private AnnouncementForm readForm(HttpServletRequest request) {
        AnnouncementForm form = new AnnouncementForm();
        form.setTarget(request.getParameter("target"));
        form.setPhone(request.getParameter("phone"));
        form.setType(request.getParameter("type"));
        form.setTitle(request.getParameter("title"));
        form.setMessage(request.getParameter("message"));
        return form;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String trim(String value) { return value == null ? "" : value.trim(); }

    private void systemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi xử lý thông báo quản lý", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }
}
