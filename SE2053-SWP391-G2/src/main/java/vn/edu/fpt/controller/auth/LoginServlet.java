package vn.edu.fpt.controller.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.LoginForm;
import vn.edu.fpt.service.LoginService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-02: Login - Đăng nhập phân quyền 4 vai trò (Admin, Manager, Staff, Customer)
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/auth/login.jsp";
    private static final String COOKIE_REMEMBER_USER = "remember_user";
    private static final int REMEMBER_COOKIE_MAX_AGE = 30 * 24 * 60 * 60; // 30 ngày

    private final LoginService loginService = new LoginService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();

        // Nếu người dùng đã đăng nhập từ trước, tự động chuyển hướng theo vai trò
        User currentUser = (User) session.getAttribute(AuthFilter.SESSION_USER);
        if (currentUser != null && "Active".equalsIgnoreCase(currentUser.getStatus())) {
            response.sendRedirect(determineRedirectUrl(request, currentUser, null));
            return;
        }

        moveFlashToRequest(session, request);

        LoginForm form = new LoginForm();
        form.setReturnUrl(request.getParameter("returnUrl"));

        // Đọc cookie Remember Me để tự động điền sẵn tài khoản nếu có
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_REMEMBER_USER.equals(c.getName())) {
                    String remembered = URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
                    form.setIdentifier(remembered);
                    form.setRememberMe(true);
                    break;
                }
            }
        }

        request.setAttribute("form", form);
        request.setAttribute("errors", new LinkedHashMap<>());
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Kiểm tra an toàn bảo mật CSRF
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        LoginForm form = readForm(request);

        try {
            // Bước 1: Xác thực tài khoản, mật khẩu và trạng thái hoạt động
            User user = loginService.authenticate(form);

            // Bước 2: Chống tấn công Session Fixation bằng cách làm mới Session ID
            request.changeSessionId();

            // Bước 3: Lưu đối tượng User vào Session
            HttpSession session = request.getSession();
            session.setAttribute(AuthFilter.SESSION_USER, user);
            ActivityLogger.info(request, LogEventType.AUTH, "Đăng nhập thành công", "User", user.getUserId(), null);

            // Bước 4: Xử lý Cookie Ghi nhớ đăng nhập
            handleRememberCookie(request, response, form, user);

            // Bước 5: Thông báo thành công và Chuyển hướng phân quyền theo 4 vai trò
            flash(request, "flashSuccess", Messages.format(Messages.LOGIN_SUCCESS, user.getFullName()));
            String targetUrl = determineRedirectUrl(request, user, form.getReturnUrl());
            response.sendRedirect(targetUrl);

        } catch (ValidationException e) {
            // Đăng nhập thất bại: hiển thị lại form kèm lỗi
            if (e.getErrors().containsKey("auth")) {
                // UC-32: chỉ ghi lần thử sai tài khoản/mật khẩu hoặc tài khoản bị khóa, không ghi lỗi bỏ trống ô
                ActivityLogger.warningForEmail(request, form.getIdentifier(), LogEventType.AUTH,
                        "Đăng nhập thất bại", e.getErrors().get("auth"));
            }
            HttpSession session = request.getSession();
            form.setPassword(""); // Xóa mật khẩu vì lý do an toàn
            request.setAttribute("form", form);
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi đăng nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private LoginForm readForm(HttpServletRequest request) {
        LoginForm form = new LoginForm();
        form.setIdentifier(request.getParameter("identifier"));
        form.setPassword(request.getParameter("password"));
        String remember = request.getParameter("rememberMe");
        form.setRememberMe("on".equalsIgnoreCase(remember) || "true".equalsIgnoreCase(remember) || "1".equals(remember));
        form.setReturnUrl(request.getParameter("returnUrl"));
        return form;
    }

    private void handleRememberCookie(HttpServletRequest request, HttpServletResponse response, LoginForm form, User user) {
        String cookiePath = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        if (form.isRememberMe()) {
            Cookie cookie = new Cookie(COOKIE_REMEMBER_USER, URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8));
            cookie.setMaxAge(REMEMBER_COOKIE_MAX_AGE);
            cookie.setPath(cookiePath);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        } else {
            // Hủy cookie nếu người dùng bỏ tích
            Cookie cookie = new Cookie(COOKIE_REMEMBER_USER, "");
            cookie.setMaxAge(0);
            cookie.setPath(cookiePath);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
    }

    /**
     * Xác định URL chuyển hướng sau khi đăng nhập:
     * - Ưu tiên returnUrl hợp lệ nếu có (và phù hợp quyền hạn)
     * - Phân quyền theo 4 vai trò:
     *   1. Admin -> /admin/users (Quản lý tài khoản & phân quyền)
     *   2. Manager -> /manager/movies (Quản lý nghiệp vụ rạp)
     *   3. Staff -> /staff/dashboard
     *   4. Customer -> / (trang chủ)
     */
    public static String determineRedirectUrl(HttpServletRequest request, User user, String returnUrl) {
        String ctx = request.getContextPath();

        // 1. Kiểm tra returnUrl hợp lệ (chống lỗ hổng Open Redirect)
        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            String trimmed = returnUrl.trim();
            // Nếu returnUrl bắt đầu bằng contextPath hoặc dấu gạch chéo an toàn (không phải //)
            if (trimmed.startsWith("/") && !trimmed.startsWith("//") && !trimmed.startsWith("/\\")) {
                // Kiểm tra phân quyền nhanh cho returnUrl
                boolean isAdminArea = trimmed.contains("/admin/");
                boolean isManagerArea = trimmed.contains("/manager/");
                boolean isStaffArea = trimmed.contains("/staff/");
                String role = user.getRole();

                if (isAdminArea && Role.ADMIN.equals(role)) {
                    return trimmed.startsWith(ctx) ? trimmed : ctx + trimmed;
                }
                if (isManagerArea && Role.MANAGER.equals(role)) {
                    return trimmed.startsWith(ctx) ? trimmed : ctx + trimmed;
                }
                if (isStaffArea && (Role.STAFF.equals(role) || Role.MANAGER.equals(role))) {
                    return trimmed.startsWith(ctx) ? trimmed : ctx + trimmed;
                }
                if (!isAdminArea && !isManagerArea && !isStaffArea) {
                    return trimmed.startsWith(ctx) ? trimmed : ctx + trimmed;
                }
            }
        }

        // 2. Chuyển hướng mặc định theo 4 vai trò:
        // Admin quản lý tài khoản & phân quyền; Manager quản lý nghiệp vụ rạp
        String role = user.getRole() == null ? Role.CUSTOMER : user.getRole();
        return switch (role) {
            case Role.ADMIN -> ctx + "/admin/users";
            case Role.MANAGER -> ctx + "/manager/movies";
            case Role.STAFF -> ctx + "/staff/dashboard";
            case Role.CUSTOMER -> ctx + "/";
            default -> ctx + "/";
        };
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
