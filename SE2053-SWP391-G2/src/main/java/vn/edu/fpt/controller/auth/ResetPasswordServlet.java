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
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.PasswordResetService;
import vn.edu.fpt.service.ResetPasswordForm;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-03: Reset Password - Nhập mật khẩu mới từ đường link khôi phục trong email
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ResetPasswordServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/auth/reset-password.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        String token = request.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            flash(request, FLASH_ERROR, Messages.RESET_PASSWORD_TOKEN_INVALID);
            response.sendRedirect(request.getContextPath() + "/forgot-password");
            return;
        }

        try {
            // Xác thực token hợp lệ trước khi hiển thị form đặt mật khẩu
            passwordResetService.validateToken(token.trim());

            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token.trim());

            request.setAttribute("form", form);
            request.setAttribute("errors", new LinkedHashMap<>());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (ValidationException e) {
            flash(request, FLASH_ERROR, Messages.RESET_PASSWORD_TOKEN_INVALID);
            response.sendRedirect(request.getContextPath() + "/forgot-password");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi kiểm tra token khôi phục mật khẩu", e);
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

        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(request.getParameter("token"));
        form.setNewPassword(request.getParameter("newPassword"));
        form.setConfirmPassword(request.getParameter("confirmPassword"));

        try {
            // Thực hiện đặt lại mật khẩu mới
            User user = passwordResetService.resetPassword(form);
            ActivityLogger.infoForEmail(request, user.getEmail(), LogEventType.AUTH, "Đặt lại mật khẩu qua email",
                    "User", user.getUserId(), null);

            // Đổi mật khẩu thành công -> chuyển về trang đăng nhập
            flash(request, FLASH_SUCCESS, Messages.RESET_PASSWORD_SUCCESS);
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (ValidationException e) {
            if (e.getErrors().containsKey("token") || e.getErrors().containsKey("general")) {
                String errorMsg = e.getErrors().getOrDefault("token", e.getErrors().get("general"));
                flash(request, FLASH_ERROR, errorMsg);
                response.sendRedirect(request.getContextPath() + "/forgot-password");
                return;
            }

            HttpSession session = request.getSession();
            request.setAttribute("form", form);
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi đặt lại mật khẩu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
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
