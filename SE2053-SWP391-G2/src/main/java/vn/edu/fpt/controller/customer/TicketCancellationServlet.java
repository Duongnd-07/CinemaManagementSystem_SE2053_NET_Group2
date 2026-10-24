package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.CancellableTicket;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.TicketCancellationService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

@WebServlet("/customer/cancel-ticket")
public class TicketCancellationServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(TicketCancellationServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/cancel-ticket.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private final TicketCancellationService cancellationService = new TicketCancellationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int ticketId = parseTicketId(request);
        if (ticketId < 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User user = (User) request.getSession(false).getAttribute("user");
        try {
            CancellableTicket ticket = cancellationService.getForConfirmation(ticketId, user.getUserId());
            request.setAttribute("ticket", ticket);
            request.setAttribute("csrfToken", CsrfToken.get(request.getSession()));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().get("general"));
            response.sendRedirect(request.getContextPath() + "/customer/bookings");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kiểm tra yêu cầu hủy vé", e);
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
        int ticketId = parseTicketId(request);
        if (ticketId < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        User user = (User) request.getSession(false).getAttribute("user");
        try {
            cancellationService.cancel(ticketId, user.getUserId());
            ActivityLogger.info(request, LogEventType.BOOKING, "Hủy vé online", "Ticket", ticketId,
                    "Hoàn voucher tài khoản theo chính sách hủy vé");
            request.getSession().setAttribute(FLASH_SUCCESS, Messages.TICKET_CANCELLATION_SUCCESS);
            response.sendRedirect(request.getContextPath() + "/customer/bookings");
        } catch (ValidationException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getErrors().getOrDefault(
                    "general", Messages.CUSTOMER_ACTION_FAILED));
            response.sendRedirect(request.getContextPath() + "/customer/bookings");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi hủy vé online", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    private int parseTicketId(HttpServletRequest request) {
        try {
            return Integer.parseInt(request.getParameter("ticketId"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
