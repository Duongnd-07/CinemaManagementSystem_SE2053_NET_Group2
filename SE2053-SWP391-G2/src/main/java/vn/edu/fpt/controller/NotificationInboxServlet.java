package vn.edu.fpt.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.CustomerNotificationService;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

@WebServlet({"/staff/notifications", "/manager/inbox", "/admin/notifications"})
public class NotificationInboxServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(NotificationInboxServlet.class.getName());
    private final CustomerNotificationService notificationService = new CustomerNotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            Object flash = session.getAttribute("flashSuccess");
            if (flash != null) {
                request.setAttribute("flashSuccess", flash);
                session.removeAttribute("flashSuccess");
            }
            int page;
            try { page = Integer.parseInt(request.getParameter("page")); }
            catch (NumberFormatException e) { page = 1; }
            request.setAttribute("notifications", notificationService.openForUser(user.getUserId(), page));
            // Filter đã đếm trước khi đánh dấu đã xem; đếm lại để chuông trên chính trang này đúng
            request.setAttribute("unreadNotificationCount", notificationService.countUnread(user.getUserId()));
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.setAttribute("emptyMessage", Messages.NOTIFICATION_EMPTY);
            request.setAttribute("activeMenu", "inbox");
            request.setAttribute("inboxUrl", request.getContextPath() + request.getServletPath());
            String view = switch (request.getServletPath()) {
                case "/manager/inbox" -> "/WEB-INF/views/manager/inbox.jsp";
                case "/admin/notifications" -> "/WEB-INF/views/admin/notifications.jsp";
                default -> "/WEB-INF/views/staff/notifications.jsp";
            };
            request.getRequestDispatcher(view).forward(request, response);
        } catch (SQLException e) {
            systemError(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"markAllRead".equals(request.getParameter("action"))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            notificationService.markAllRead(user.getUserId());
            session.setAttribute("flashSuccess", Messages.NOTIFICATION_MARKED_READ);
            response.sendRedirect(request.getContextPath() + request.getServletPath());
        } catch (SQLException e) {
            systemError(response, e);
        }
    }

    private void systemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi xử lý hộp thư thông báo", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }
}
