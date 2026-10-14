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
import java.util.List;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.util.AppConfig;

// RBAC theo tiền tố URL: /staff/* cho Staff, /manager/* cho Manager và Admin, /customer/* cho Customer
@WebFilter({"/staff/*", "/manager/*", "/customer/*"})
public class AuthFilter implements Filter {
    public static final String SESSION_USER = "user";
    private static final String ACTIVE = "Active";
    private static final List<String> STAFF_ROLES = List.of(Role.STAFF, Role.MANAGER, Role.ADMIN);
    private static final List<String> MANAGER_ROLES = List.of(Role.MANAGER, Role.ADMIN);
    private static final List<String> CUSTOMER_ROLES = List.of(Role.CUSTOMER);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        boolean staffArea = req.getServletPath().startsWith("/staff");
        boolean customerArea = req.getServletPath().startsWith("/customer");
        List<String> allowedRoles = customerArea ? CUSTOMER_ROLES : staffArea ? STAFF_ROLES : MANAGER_ROLES;

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
            String returnUrl = req.getRequestURI();
            if (req.getQueryString() != null) {
                returnUrl += "?" + req.getQueryString();
            }
            resp.sendRedirect(req.getContextPath() + "/login?returnUrl=" + java.net.URLEncoder.encode(returnUrl, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        if (!allowed) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập vào khu vực này.");
            return;
        }
        chain.doFilter(request, response);
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
