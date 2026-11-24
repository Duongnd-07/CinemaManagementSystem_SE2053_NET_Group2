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

    private static final RolePermissionService PERMISSIONS = new RolePermissionService();

    private final UserDAO userDAO = new UserDAO();

    /**
     * Vai trò có được vào URL này không. Dùng chung cho filter và cho returnUrl sau đăng nhập (UC-02 A1)
     * để hai nơi không lệch nhau: chức năng trong danh mục UC-31 xét theo phân quyền của Admin,
     * URL khác theo khu vực; URL ngoài 4 khu vực là trang công khai.
     *
     * @param servletPath đường dẫn sau context path, không kèm query string
     */
    public static boolean canAccess(String servletPath, String role) throws SQLException {
        List<String> roles = areaRoles(servletPath);
        if (roles == null) {
            return true;
        }
        Permission permission = Permission.fromPath(servletPath);
        return permission != null ? PERMISSIONS.isAllowed(role, permission) : roles.contains(role);
    }

    // null: URL không thuộc khu vực nào cần đăng nhập
    private static List<String> areaRoles(String servletPath) {
        if (inArea(servletPath, "/admin")) {
            return ADMIN_ROLES;
        }
        if (inArea(servletPath, "/manager")) {
            return MANAGER_ROLES;
        }
        if (inArea(servletPath, "/staff")) {
            return STAFF_ROLES;
        }
        if (inArea(servletPath, "/customer")) {
            return CUSTOMER_ROLES;
        }
        return null;
    }

    private static boolean inArea(String servletPath, String prefix) {
        return servletPath.equals(prefix) || servletPath.startsWith(prefix + "/");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        boolean managerArea = inArea(req.getServletPath(), "/manager");
        boolean staffArea = inArea(req.getServletPath(), "/staff");
        boolean customerArea = inArea(req.getServletPath(), "/customer");
        List<String> allowedRoles = areaRoles(req.getServletPath());

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
        // UC-31: chức năng trong danh mục do Admin phân quyền; URL ngoài danh mục giữ quy tắc theo tiền tố
        boolean permitted;
        try {
            permitted = canAccess(req.getServletPath(), dbUser.getRole());
            if (permitted && (managerArea || staffArea)) {
                req.setAttribute(ALLOWED_PERMISSIONS, PERMISSIONS.allowedFor(dbUser.getRole()).stream()
                        .map(Permission::name).collect(Collectors.toSet()));
                req.setAttribute("managementHome", PERMISSIONS.firstManagementPath(dbUser.getRole()));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi kiểm tra quyền truy cập", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
            return;
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
