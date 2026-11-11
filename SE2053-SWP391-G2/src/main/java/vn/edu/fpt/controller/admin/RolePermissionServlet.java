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
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.RolePermissionService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-31: Manage Roles & Permissions. AuthFilter đã giới hạn /admin/* cho Admin.
@WebServlet("/admin/permissions")
public class RolePermissionServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RolePermissionServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/admin/role-permissions.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final RolePermissionService permissionService = new RolePermissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);
        try {
            request.setAttribute("groups", permissionService.loadRows());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
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
            List<RolePermissionService.Change> changes = permissionService.save(request.getParameterValues("grant"));
            for (RolePermissionService.Change change : changes) {
                // UC-32: mỗi quyền đổi là một dòng nhật ký
                ActivityLogger.info(request, LogEventType.CONFIG, change.granted() ? "Cấp quyền" : "Thu hồi quyền",
                        "RolePermission", change.permission().keyFor(change.role()),
                        change.role() + ": " + change.permission().getLabel());
            }
            request.getSession().setAttribute(FLASH_SUCCESS, changes.isEmpty()
                    ? Messages.PERMISSION_NO_CHANGES : Messages.format(Messages.PERMISSION_SAVED, changes.size()));
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, Messages.PERMISSION_INVALID);
        } catch (SQLException e) {
            // Service đã rollback
            handleSystemError(response, e);
            return;
        }
        // Post/Redirect/Get để F5 không gửi lại form
        response.sendRedirect(request.getContextPath() + "/admin/permissions");
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu ở trang phân quyền", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
