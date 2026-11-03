package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LoyaltyTransaction;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.LoyaltyPointsService;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/customer/loyalty")
public class LoyaltyPointsServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoyaltyPointsServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/loyalty.jsp";
    private final LoyaltyPointsService loyaltyPointsService = new LoyaltyPointsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        try {
            int userId = user == null ? 0 : user.getUserId();
            Integer balance = loyaltyPointsService.findBalance(userId);
            if (balance == null) {
                response.sendRedirect(request.getContextPath() + "/logout");
                return;
            }
            Page<LoyaltyTransaction> history = loyaltyPointsService.findHistory(userId, parsePage(request));
            request.setAttribute("pointBalance", balance);
            request.setAttribute("history", history);
            request.setAttribute("emptyMessage", Messages.LOYALTY_HISTORY_EMPTY);
            request.setAttribute("activeNav", "loyalty");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải lịch sử tích điểm", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private int parsePage(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
