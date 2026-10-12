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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.service.RegisterForm;
import vn.edu.fpt.service.RegisterService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-01: Xác thực mã OTP qua Email để hoàn tất đăng ký tài khoản thành viên
@WebServlet("/verify-email")
public class VerifyEmailServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(VerifyEmailServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/auth/verify-email.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    private final RegisterService registerService = new RegisterService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        RegisterForm form = (RegisterForm) session.getAttribute(RegisterServlet.SESSION_PENDING_REGISTER);

        // Nếu không có phiên đăng ký đang chờ (vào trực tiếp URL), chuyển về trang đăng ký
        if (form == null) {
            flash(request, FLASH_ERROR, Messages.REGISTER_SESSION_EXPIRED);
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        moveFlashToRequest(session, request);
        request.setAttribute("email", form.getEmail());
        request.setAttribute("fullName", form.getFullName());
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

        HttpSession session = request.getSession();
        RegisterForm form = (RegisterForm) session.getAttribute(RegisterServlet.SESSION_PENDING_REGISTER);

        if (form == null) {
            flash(request, FLASH_ERROR, Messages.REGISTER_SESSION_EXPIRED);
            response.sendRedirect(request.getContextPath() + "/register");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("resend".equals(action)) {
                // Người dùng bấm "Gửi lại mã OTP"
                registerService.resendOtp(form);
                flash(request, FLASH_SUCCESS, Messages.REGISTER_OTP_RESENT);
                response.sendRedirect(request.getContextPath() + "/verify-email");
            } else {
                // Người dùng nhập OTP để hoàn tất đăng ký
                String otp = request.getParameter("otp");
                registerService.verifyOtpAndComplete(form, otp);

                // Đăng ký thành công -> Xóa dữ liệu tạm khỏi Session
                session.removeAttribute(RegisterServlet.SESSION_PENDING_REGISTER);

                flash(request, FLASH_SUCCESS, Messages.REGISTER_SUCCESS);
                // Redirect sang trang đăng nhập (UC-02) hoặc thông báo thành công
                response.sendRedirect(request.getContextPath() + "/login");
            }
        } catch (ValidationException e) {
            // Mã OTP sai hoặc hết hạn: hiển thị lỗi ngay dưới ô nhập
            request.setAttribute("email", form.getEmail());
            request.setAttribute("fullName", form.getFullName());
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi xác minh OTP", e);
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
