package vn.edu.fpt.controller.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.ForgotPasswordForm;
import vn.edu.fpt.service.PasswordResetService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-03: Forgot Password - Gửi đường link khôi phục mật khẩu qua email
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/auth/forgot-password.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String FLASH_SENT_EMAIL = "sentEmail";

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        ForgotPasswordForm form = new ForgotPasswordForm();
        request.setAttribute("form", form);
        request.setAttribute("errors", new LinkedHashMap<>());
        request.setAttribute("csrfToken", CsrfToken.get(session));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ForgotPasswordForm form = new ForgotPasswordForm();
        form.setEmail(request.getParameter("email"));

        try {
            // Xác định base URL của ứng dụng
            String appBaseUrl = buildAppBaseUrl(request);

            // Bắt đầu quy trình gửi link khôi phục
            passwordResetService.initiatePasswordReset(form, appBaseUrl);

            // Thông báo thành công và chuyển hướng (PRG Pattern)
            flash(request, FLASH_SUCCESS, Messages.FORGOT_PASSWORD_EMAIL_SENT);
            flash(request, FLASH_SENT_EMAIL, form.getEmail());
            response.sendRedirect(request.getContextPath() + "/forgot-password");
        } catch (ValidationException e) {
            // Có lỗi validate: hiển thị lại form kèm thông báo lỗi
            HttpSession session = request.getSession();
            request.setAttribute("form", form);
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi yêu cầu khôi phục mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private String buildAppBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);

        return scheme + "://" + serverName + (isDefaultPort ? "" : (":" + serverPort)) + contextPath;
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR, FLASH_SENT_EMAIL)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
