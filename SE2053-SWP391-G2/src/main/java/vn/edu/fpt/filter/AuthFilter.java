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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import vn.edu.fpt.model.Permission;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.User;
import vn.edu.fpt.dao.UserDAO;
import vn.edu.fpt.service.RolePermissionService;
import vn.edu.fpt.util.AppConfig;
import vn.edu.fpt.util.Messages;

// RBAC theo tiền tố URL: /admin/* cho Admin, /manager/* cho Manager, /staff/* cho Staff, /customer/* cho Customer.
// Các chức năng của Manager và Staff nằm trong enum Permission được kiểm tra theo cấu hình của Admin (UC-31).
@WebFilter({"/admin/*", "/staff/*", "/manager/*", "/customer/*"})
public class AuthFilter implements Filter {
    public static final String SESSION_USER = "user";
    private static final String ACTIVE = "Active";
    private static final List<String> ADMIN_ROLES = List.of(Role.ADMIN);
    private static final List<String> MANAGER_ROLES = List.of(Role.MANAGER);
    private static final List<String> STAFF_ROLES = List.of(Role.STAFF, Role.MANAGER);
    private static final List<String> CUSTOMER_ROLES = List.of(Role.CUSTOMER);
    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    // Tên các quyền của người dùng hiện tại, JSP dùng để ẩn menu không được phép
    private static final String ALLOWED_PERMISSIONS = "allowedPermissions";

    private final UserDAO userDAO = new UserDAO();
    private final RolePermissionService permissionService = new RolePermissionService();

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
            if (managerArea || staffArea) {
                req.setAttribute(ALLOWED_PERMISSIONS, EnumSet.allOf(Permission.class).stream()
                        .map(Permission::name).collect(Collectors.toSet()));
            }
            chain.doFilter(request, response);
            return;
        }
        if (user == null) {
            redirectToLogin(req, resp);
            return;
        }
        User dbUser;
        try {
            dbUser = userDAO.findById(user.getUserId());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi kiểm tra hiệu lực phiên đăng nhập", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
            return;
        }
        // Không chuyển quyền cho phiên cũ khi Admin khóa tài khoản hoặc đổi vai trò.
        if (dbUser == null || !ACTIVE.equals(dbUser.getStatus())
                || !Objects.equals(user.getRole(), dbUser.getRole())
                || user.getPasswordHash() == null
                || !user.getPasswordHash().equals(dbUser.getPasswordHash())) {
            session.invalidate();
            req.getSession(true).setAttribute("flashError", Messages.SESSION_REVOKED);
            redirectToLogin(req, resp);
            return;
        }
        boolean permitted = allowedRoles.contains(dbUser.getRole());
        if (managerArea || staffArea) {
            // UC-31: chức năng trong danh mục do Admin phân quyền; URL ngoài danh mục giữ quy tắc theo tiền tố
            try {
                Permission permission = Permission.fromPath(req.getServletPath());
                if (permission != null) {
                    permitted = permissionService.isAllowed(dbUser.getRole(), permission);
                }
                if (permitted) {
                    req.setAttribute(ALLOWED_PERMISSIONS, permissionService.allowedFor(dbUser.getRole()).stream()
                            .map(Permission::name).collect(Collectors.toSet()));
                    req.setAttribute("managementHome", permissionService.firstManagementPath(dbUser.getRole()));
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi kiểm tra quyền truy cập", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
                return;
            }
        }
        if (!permitted) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập vào khu vực này.");
            return;
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
