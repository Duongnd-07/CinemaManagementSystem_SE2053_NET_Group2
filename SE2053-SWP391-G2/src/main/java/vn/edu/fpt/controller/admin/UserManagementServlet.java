package vn.edu.fpt.controller.admin;

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
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.UserManagementService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

/**
 * Quản lý tài khoản & Phân quyền hệ thống dành cho Admin (UC-30).
 * Cho phép xem danh sách, lọc, phân quyền, đổi trạng thái, cập nhật thông tin và cấp lại mật khẩu.
 */
@WebServlet("/admin/users")
public class UserManagementServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserManagementServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/admin/users.jsp";
    private static final int PAGE_SIZE = 10;

    private final UserManagementService userManagementService = new UserManagementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        String keyword = request.getParameter("keyword");
        String role = request.getParameter("role");
        String status = request.getParameter("status");

        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Math.max(1, Integer.parseInt(pageParam.trim()));
            }
        } catch (NumberFormatException ignored) {
            page = 1;
        }

        try {
            Map<String, Integer> stats = userManagementService.getStats();
            Page<User> userPage = userManagementService.listUsers(keyword, role, status, page, PAGE_SIZE);

            request.setAttribute("users", userPage.getItems());
            request.setAttribute("stats", stats);
            request.setAttribute("keyword", keyword == null ? "" : keyword.trim());
            request.setAttribute("selectedRole", role == null ? "ALL" : role.trim());
            request.setAttribute("selectedStatus", status == null ? "ALL" : status.trim());
            request.setAttribute("currentPage", userPage.getPage());
            request.setAttribute("totalPages", userPage.getTotalPages());
            request.setAttribute("totalRecords", userPage.getTotalItems());
            request.setAttribute("csrfToken", CsrfToken.get(session));

            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi tải danh sách người dùng cho Admin", e);
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

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute(AuthFilter.SESSION_USER);
        int currentAdminId = currentUser != null ? currentUser.getUserId() : 0;

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        try {
            switch (action) {
                case "create-user" -> {
                    String fullName = request.getParameter("fullName");
                    String email = request.getParameter("email");
                    String phone = request.getParameter("phone");
                    String role = request.getParameter("role");
                    String password = request.getParameter("password");
                    User created = userManagementService.createUser(fullName, email, phone, role, password);
                    ActivityLogger.info(request, LogEventType.ACCOUNT, "Tạo tài khoản", "User",
                            created == null ? null : created.getUserId(), email + " - vai trò " + role);
                    flash(request, "flashSuccess", Messages.USER_CREATED);
                }
                case "update-profile" -> {
                    int targetUserId = Integer.parseInt(request.getParameter("userId"));
                    String fullName = request.getParameter("fullName");
                    String phone = request.getParameter("phone");
                    userManagementService.updateUserProfile(targetUserId, fullName, phone);
                    ActivityLogger.info(request, LogEventType.ACCOUNT, "Cập nhật thông tin tài khoản", "User",
                            targetUserId, "Họ tên: " + fullName + ", SĐT: " + phone);
                    flash(request, "flashSuccess", Messages.USER_PROFILE_UPDATED);
                }
                case "change-role" -> {
                    int targetUserId = Integer.parseInt(request.getParameter("userId"));
                    String newRole = request.getParameter("newRole");
                    userManagementService.updateUserRole(currentAdminId, targetUserId, newRole);
                    ActivityLogger.info(request, LogEventType.ACCOUNT, "Đổi vai trò tài khoản", "User",
                            targetUserId, "Vai trò mới: " + newRole);
                    flash(request, "flashSuccess", Messages.USER_ROLE_UPDATED);
                }
                case "change-status" -> {
                    int targetUserId = Integer.parseInt(request.getParameter("userId"));
                    String newStatus = request.getParameter("newStatus");
                    userManagementService.updateUserStatus(currentAdminId, targetUserId, newStatus);
                    ActivityLogger.info(request, LogEventType.ACCOUNT, "Đổi trạng thái tài khoản", "User",
                            targetUserId, "Trạng thái mới: " + newStatus);
                    flash(request, "flashSuccess", Messages.USER_STATUS_UPDATED);
                }
                case "reset-password" -> {
                    int targetUserId = Integer.parseInt(request.getParameter("userId"));
                    String newPassword = request.getParameter("newPassword");
                    userManagementService.resetPassword(targetUserId, newPassword);
                    // BR-06: không ghi mật khẩu mới
                    ActivityLogger.info(request, LogEventType.ACCOUNT, "Admin đặt lại mật khẩu", "User",
                            targetUserId, null);
                    flash(request, "flashSuccess", Messages.USER_PASSWORD_RESET);
                }
                default -> flash(request, "flashError", "Hành động không hợp lệ.");
            }
        } catch (ValidationException e) {
            String firstError = e.getErrors().values().stream().findFirst().orElse("Dữ liệu không hợp lệ.");
            flash(request, "flashError", firstError);
        } catch (NumberFormatException e) {
            flash(request, "flashError", "Mã tài khoản không hợp lệ.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi thao tác quản lý người dùng", e);
            flash(request, "flashError", "Lỗi cơ sở dữ liệu. Vui lòng thử lại sau.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of("flashSuccess", "flashError")) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
