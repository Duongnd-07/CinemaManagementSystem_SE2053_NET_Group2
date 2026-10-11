package vn.edu.fpt.controller.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.util.Messages;

// UC-02: Logout - Đăng xuất an toàn khỏi hệ thống (hủy Session & Cookie)
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final String COOKIE_REMEMBER_USER = "remember_user";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processLogout(request, response);
    }

    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 1. Hủy cookie Remember Me nếu có
        String cookiePath = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
        Cookie cookie = new Cookie(COOKIE_REMEMBER_USER, "");
        cookie.setMaxAge(0);
        cookie.setPath(cookiePath);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // 2. Hủy session hiện tại và xóa sạch thông tin người dùng
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(AuthFilter.SESSION_USER);
            session.invalidate();
        }

        // 3. Khởi tạo session mới để mang thông điệp thông báo đăng xuất thành công
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("flashSuccess", Messages.LOGOUT_SUCCESS);

        // 4. Redirect về trang đăng nhập
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
