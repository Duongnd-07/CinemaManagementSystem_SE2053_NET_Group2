package vn.edu.fpt.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.CustomerNotificationService;

@WebFilter("/*")
public class UnreadNotificationFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(UnreadNotificationFilter.class.getName());
    private final CustomerNotificationService notificationService = new CustomerNotificationService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getServletPath();
        // Bỏ qua file tĩnh và endpoint JSON gợi ý phim (gọi theo từng phím gõ, không render header)
        if ("GET".equals(req.getMethod()) && !path.startsWith("/assets/") && !"/movies/suggest".equals(path)) {
            HttpSession session = req.getSession(false);
            User user = session == null ? null : (User) session.getAttribute("user");
            if (user != null) {
                try {
                    request.setAttribute("unreadNotificationCount", notificationService.countUnread(user.getUserId()));
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Không đếm được thông báo chưa đọc", e);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
