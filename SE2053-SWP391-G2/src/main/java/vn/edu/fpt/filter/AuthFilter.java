package vn.edu.fpt.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ProfileService;
import vn.edu.fpt.util.AppConfig;
import vn.edu.fpt.util.Messages;

// RBAC theo tiền tố URL: /admin/* cho Admin, /manager/* cho Manager, /staff/* cho Staff, /customer/* cho Customer
@WebFilter({"/admin/*", "/staff/*", "/manager/*", "/customer/*"})
public class AuthFilter implements Filter {
    public static final String SESSION_USER = "user";
    private static final String ACTIVE = "Active";
    private static final List<String> ADMIN_ROLES = List.of(Role.ADMIN);
    private static final List<String> MANAGER_ROLES = List.of(Role.MANAGER);
    private static final List<String> STAFF_ROLES = List.of(Role.STAFF, Role.MANAGER);
    private static final List<String> CUSTOMER_ROLES = List.of(Role.CUSTOMER);
    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    private final ProfileService profileService = new ProfileService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        boolean adminArea = req.getServletPath().startsWith("/admin");
        boolean managerArea = req.getServletPath().startsWith("/manager");
        boolean staffArea = req.getServletPath().startsWith("/staff");
        boolean customerArea = req.getServletPath().startsWith("/customer");
        List<String> allowedRoles = adminArea ? ADMIN_ROLES
                : managerArea ? MANAGER_ROLES
                : staffArea ? STAFF_ROLES
                : CUSTOMER_ROLES;

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(SESSION_USER);
        boolean allowed = user != null && ACTIVE.equals(user.getStatus()) && allowedRoles.contains(user.getRole());

        // Khu vực khách hàng luôn bắt buộc đăng nhập thật vì các UC ở đây thao tác trên chính tài khoản đó
        if (!customerArea && !AppConfig.getBoolean("auth.enabled", true)) {
            // Khi cấu hình auth.enabled=false: gắn user dev để các UC lấy được mã nhân viên/quản lý
            if (!allowed) {
                req.getSession().setAttribute(SESSION_USER, devUser(staffArea));
            }
            chain.doFilter(request, response);
            return;
        }
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }
        if (!allowed) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập vào khu vực này.");
            return;
        }
        if (customerArea) {
            boolean credentialCurrent;
            try {
                credentialCurrent = profileService.isSessionCredentialCurrent(user);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi kiểm tra hiệu lực phiên đăng nhập", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
                return;
            }
            if (!credentialCurrent) {
                // UC-10 AF1 bước 4: mật khẩu đã được đổi ở phiên khác -> thu hồi phiên này
                session.invalidate();
                req.getSession(true).setAttribute("flashError", Messages.PROFILE_SESSION_REVOKED);
                redirectToLogin(req, resp);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String returnUrl = req.getRequestURI();
        if (req.getQueryString() != null) {
            returnUrl += "?" + req.getQueryString();
        }
        resp.sendRedirect(req.getContextPath() + "/login?returnUrl=" + java.net.URLEncoder.encode(returnUrl, java.nio.charset.StandardCharsets.UTF_8));
    }

    private User devUser(boolean staffArea) {
        User user = new User();
        user.setUserId(AppConfig.getInt(staffArea ? "auth.dev.staffId" : "auth.dev.managerId", 0));
        user.setFullName(staffArea ? "Nhân viên (dev)" : "Quản lý (dev)");
        user.setRole(staffArea ? Role.STAFF : Role.MANAGER);
        user.setStatus(ACTIVE);
        return user;
    }
}
