package vn.edu.fpt.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.SystemConfigItem;
import vn.edu.fpt.service.SystemConfigParam;
import vn.edu.fpt.service.SystemConfigService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-33: Manage System Configurations. AuthFilter đã giới hạn /admin/* cho Admin.
@WebServlet("/admin/configurations")
public class SystemConfigServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(SystemConfigServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/admin/system-config.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final SystemConfigService configService = new SystemConfigService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        moveFlashToRequest(request.getSession(), request);
        try {
            render(request, response, configService.loadItems(), Map.of());
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
        Map<String, String> input = new HashMap<>();
        for (SystemConfigParam param : SystemConfigParam.values()) {
            input.put(param.getKey(), request.getParameter(param.getKey()));
        }
        User admin = (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
        int adminId = admin == null ? 0 : admin.getUserId();
        try {
            try {
                int changed = configService.save(input, adminId);
                // Post/Redirect/Get để F5 không gửi lại form
                request.getSession().setAttribute(FLASH_SUCCESS, changed == 0
                        ? Messages.CONFIG_NO_CHANGES : Messages.format(Messages.CONFIG_SAVED, changed));
                response.sendRedirect(request.getContextPath() + "/admin/configurations");
            } catch (ValidationException e) {
                // E1: giữ lại giá trị đã nhập, báo lỗi dưới từng ô, không lưu gì
                List<SystemConfigItem> items = configService.loadItems();
                configService.applyInput(items, input);
                request.setAttribute(FLASH_ERROR, e.getErrors().get(SystemConfigService.GENERAL_ERROR));
                render(request, response, items, e.getErrors());
            }
        } catch (SQLException e) {
            // E3: service đã rollback
            handleSystemError(response, e);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response,
            List<SystemConfigItem> items, Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("groups", configService.groupItems(items));
        request.setAttribute("errors", errors);
        request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu ở trang cấu hình hệ thống", e);
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
