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
import vn.edu.fpt.model.BookingHistoryEntry;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.BookingHistoryService;
import vn.edu.fpt.util.Messages;
import vn.edu.fpt.util.Page;

@WebServlet("/customer/bookings")
public class BookingHistoryServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingHistoryServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/booking-history.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private final BookingHistoryService bookingHistoryService = new BookingHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        moveFlash(session, request, FLASH_SUCCESS);
        moveFlash(session, request, FLASH_ERROR);
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            Page<BookingHistoryEntry> history = bookingHistoryService.findForCustomer(
                    user.getUserId(), parsePage(request));
            request.setAttribute("history", history);
            request.setAttribute("activeNav", "bookings");
            request.setAttribute("emptyMessage", Messages.BOOKING_HISTORY_EMPTY);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải lịch sử đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            request.setAttribute(key, value);
            session.removeAttribute(key);
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
