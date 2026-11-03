package vn.edu.fpt.controller.customer;

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
import vn.edu.fpt.model.CustomerNotification;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.CustomerNotificationService;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/customer/notifications")
public class CustomerNotificationServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CustomerNotificationServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/notifications.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private final CustomerNotificationService notificationService = new CustomerNotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Object flash = session.getAttribute(FLASH_SUCCESS);
        if (flash != null) {
            request.setAttribute(FLASH_SUCCESS, flash);
            session.removeAttribute(FLASH_SUCCESS);
        }
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            Page<CustomerNotification> notifications = notificationService.findForUser(
                    user.getUserId(), parsePage(request));
            request.setAttribute("notifications", notifications);
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.setAttribute("activeNav", "notifications");
            request.setAttribute("emptyMessage", Messages.NOTIFICATION_EMPTY);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải thông báo khách hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
            session.setAttribute(FLASH_SUCCESS, Messages.NOTIFICATION_MARKED_READ);
            response.sendRedirect(request.getContextPath() + "/customer/notifications");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật trạng thái thông báo", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private int parsePage(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
