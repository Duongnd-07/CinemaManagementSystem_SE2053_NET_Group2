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
import vn.edu.fpt.service.RegisterForm;
import vn.edu.fpt.service.RegisterService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-01: Register Account - Đăng ký tài khoản thành viên
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/auth/register.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    public static final String SESSION_PENDING_REGISTER = "pendingRegisterForm";

    private final RegisterService registerService = new RegisterService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        // Chuẩn bị form rỗng và token CSRF
        request.setAttribute("form", new RegisterForm());
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

        RegisterForm form = readForm(request);

        try {
            // Bước 1: Validate thông tin, sinh mã OTP và gửi email
            registerService.initiateRegistration(form);

            // Bước 2: Lưu form thông tin vào Session để chờ xác minh OTP
            HttpSession session = request.getSession();
            session.setAttribute(SESSION_PENDING_REGISTER, form);

            flash(request, FLASH_SUCCESS, Messages.REGISTER_OTP_SENT);
            // PRG Pattern: POST thành công -> Redirect sang trang nhập OTP
            response.sendRedirect(request.getContextPath() + "/verify-email");
        } catch (ValidationException e) {
            // Có lỗi validate: hiển thị lại form kèm danh sách lỗi
            HttpSession session = request.getSession();
            request.setAttribute("form", form);
            request.setAttribute("errors", e.getErrors());
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi đăng ký tài khoản", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private RegisterForm readForm(HttpServletRequest request) {
        RegisterForm form = new RegisterForm();
        form.setFullName(request.getParameter("fullName"));
        form.setEmail(request.getParameter("email"));
        form.setPhone(request.getParameter("phone"));
        form.setPassword(request.getParameter("password"));
        form.setConfirmPassword(request.getParameter("confirmPassword"));
        String agreed = request.getParameter("agreedTerms");
        form.setAgreedTerms("on".equalsIgnoreCase(agreed) || "true".equalsIgnoreCase(agreed) || "1".equals(agreed));
        return form;
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
