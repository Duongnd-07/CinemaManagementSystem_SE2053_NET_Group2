package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.User;

// Nghiệp vụ Cổng làm việc Nhân viên rạp (Staff Portal)
@WebServlet("/staff/dashboard")
public class StaffDashboardServlet extends HttpServlet {
    private static final String VIEW = "/WEB-INF/views/staff/dashboard.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlashToRequest(session, request);

        User user = (User) session.getAttribute(AuthFilter.SESSION_USER);
        request.setAttribute("currentUser", user);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void moveFlashToRequest(HttpSession session, HttpServletRequest request) {
        for (String key : List.of("flashSuccess", "flashError")) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
