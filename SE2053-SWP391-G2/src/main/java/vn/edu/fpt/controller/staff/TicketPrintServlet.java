package vn.edu.fpt.controller.staff;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import vn.edu.fpt.filter.AuthFilter;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.PrintBooking;
import vn.edu.fpt.model.PrintTicket;
import vn.edu.fpt.model.Role;
import vn.edu.fpt.model.Shift;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.ShiftService;
import vn.edu.fpt.service.TicketPrintService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// UC-36: Scan E-ticket & Print Paper Ticket. GET ?code=... tra cứu đơn theo mã quét được;
// POST action=print (in các vé đã chọn) / reprint (in lại một vé, bắt buộc lý do).
// Chỉ Staff đang mở ca mới được dùng. Sau khi in, lượt in được giữ trong session một lần để trang mở hộp thoại in.
@WebServlet("/staff/ticket-print")
public class TicketPrintServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(TicketPrintServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/ticket-print.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    private static final String SESSION_JOB = "ticketPrintJob";
    // Đơn vừa tra cứu: chỉ đơn này được mở lại bằng ?booking=ID (không cho tra cứu bằng mã đơn - BR-36-01)
    private static final String SESSION_BOOKING = "ticketPrintBooking";
    private static final String FORBIDDEN = "Bạn không có quyền truy cập vào khu vực này.";
    private static final String TARGET = "Booking";

    private final TicketPrintService printService = new TicketPrintService();
    private final ShiftService shiftService = new ShiftService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (!Role.STAFF.equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        try {
            moveFlashToRequest(request);
            Shift shift = shiftService.findOpenShift(user.getUserId());
            request.setAttribute("shift", shift);
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.setAttribute("activeMenu", "ticket-print");
            request.setAttribute("minReason", TicketPrintService.MIN_REASON);
            request.setAttribute("maxReason", TicketPrintService.MAX_REASON);
            if (shift != null) {
                Object job = session.getAttribute(SESSION_JOB);
                if (job != null) {
                    request.setAttribute("printJob", job);
                    session.removeAttribute(SESSION_JOB);
                }
                loadBooking(request, session);
            }
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            handleSystemError(response, e);
        }
    }

    private void loadBooking(HttpServletRequest request, HttpSession session) throws SQLException {
        String code = request.getParameter("code");
        try {
            PrintBooking booking = null;
            if (code != null && !code.isBlank()) {
                request.setAttribute("code", code.trim());
                booking = printService.lookup(code);
            } else if (request.getParameter("booking") != null) {
                Object last = session.getAttribute(SESSION_BOOKING);
                int bookingId = parseInt(request.getParameter("booking"));
                if (last instanceof Integer id && id == bookingId) {
                    booking = printService.loadBooking(bookingId);
                }
            }
            if (booking != null) {
                session.setAttribute(SESSION_BOOKING, booking.getBookingId());
                request.setAttribute("booking", booking);
            }
        } catch (ValidationException e) {
            request.setAttribute("lookupError", e.getErrors().get("general"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (!Role.STAFF.equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN);
            return;
        }
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        int bookingId = parseInt(request.getParameter("bookingId"));
        try {
            PrintBooking job;
            if ("reprint".equals(request.getParameter("action"))) {
                String reason = request.getParameter("reason");
                job = printService.reprint(parseInt(request.getParameter("ticketId")), reason, user.getUserId(),
                        user.getFullName());
                PrintTicket ticket = job.getTickets().get(0);
                ActivityLogger.info(request, LogEventType.OPERATION, "In lại vé giấy", TARGET, job.getBookingId(),
                        "Vé " + ticket.getTicketCode() + " (ghế " + ticket.getSeatLabel() + "), lần in lại "
                                + ticket.getReprintNo() + ", lý do: " + reason.trim());
                session.setAttribute(FLASH_SUCCESS, printService.reprintedMessage(ticket.getTicketCode()));
            } else {
                job = printService.print(bookingId, selectedTickets(request), user.getUserId(), user.getFullName());
                ActivityLogger.info(request, LogEventType.OPERATION, "In vé giấy", TARGET, job.getBookingId(),
                        "Đơn #" + job.getBookingId() + ": in " + job.getTickets().size() + " vé ("
                                + job.getTickets().stream().map(t -> t.getTicketCode() + " ghế " + t.getSeatLabel())
                                .collect(Collectors.joining(", ")) + ")"
                                + (job.isConcessionSlip() ? " kèm phiếu nhận bắp nước" : ""));
                session.setAttribute(FLASH_SUCCESS, printService.printedMessage(job.getTickets().size()));
            }
            session.setAttribute(SESSION_JOB, job);
            session.setAttribute(SESSION_BOOKING, job.getBookingId());
            bookingId = job.getBookingId();
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERROR, e.getErrors().get("general"));
        } catch (SQLException e) {
            handleSystemError(response, e);
            return;
        }
        Object last = session.getAttribute(SESSION_BOOKING);
        String query = last instanceof Integer id && (bookingId <= 0 || id == bookingId) ? "?booking=" + id : "";
        response.sendRedirect(request.getContextPath() + "/staff/ticket-print" + query);
    }

    private List<Integer> selectedTickets(HttpServletRequest request) {
        List<Integer> ids = new ArrayList<>();
        String[] values = request.getParameterValues("ticketIds");
        if (values != null) {
            for (String value : values) {
                int id = parseInt(value);
                if (id > 0) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private User currentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(AuthFilter.SESSION_USER);
    }

    private void moveFlashToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession();
        for (String key : List.of(FLASH_SUCCESS, FLASH_ERROR)) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    private void handleSystemError(HttpServletResponse response, SQLException e) throws IOException {
        LOGGER.log(Level.SEVERE, "Lỗi truy cập dữ liệu in vé giấy", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
