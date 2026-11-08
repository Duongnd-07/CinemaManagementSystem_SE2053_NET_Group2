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
import java.util.UUID;
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
// POST action=print (in các vé đã chọn) / reprint (in lại một vé, bắt buộc lý do) chỉ chuẩn bị lượt in và mở hộp
// thoại in, CHƯA ghi DB; action=confirm (nhân viên xác nhận vé đã ra giấy) mới ghi lượt in, action=cancel bỏ lượt in.
// Trình duyệt không cho biết đã bấm In hay Hủy trong hộp thoại in, nên hủy / bấm Back / rời trang đều không làm vé bị
// tính là đã in. Lượt in chưa xác nhận được giữ trong session và trang nhắc xác nhận cho tới khi nhân viên trả lời.
// Chỉ Staff đang mở ca mới được dùng.
@WebServlet("/staff/ticket-print")
public class TicketPrintServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(TicketPrintServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/staff/ticket-print.jsp";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";
    // Lượt in đang chờ xác nhận và token của nó (form xác nhận / hủy phải gửi đúng token)
    private static final String SESSION_PENDING = "ticketPrintPending";
    private static final String SESSION_PENDING_TOKEN = "ticketPrintPendingToken";
    // Lượt in vừa tạo: trang tự mở hộp thoại in một lần
    private static final String SESSION_AUTO_PRINT = "ticketPrintAutoPrint";
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
                Object pending = session.getAttribute(SESSION_PENDING);
                if (pending instanceof PrintBooking) {
                    request.setAttribute("printJob", pending);
                    request.setAttribute("printToken", session.getAttribute(SESSION_PENDING_TOKEN));
                    request.setAttribute("autoPrint", session.getAttribute(SESSION_AUTO_PRINT) != null);
                    session.removeAttribute(SESSION_AUTO_PRINT);
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
        String action = request.getParameter("action");
        try {
            if ("confirm".equals(action) || "cancel".equals(action)) {
                finishPending(request, session, user, "confirm".equals(action));
            } else {
                PrintBooking job = "reprint".equals(action)
                        ? printService.prepareReprint(parseInt(request.getParameter("ticketId")),
                                request.getParameter("reason"), user.getUserId(), user.getFullName())
                        : printService.preparePrint(parseInt(request.getParameter("bookingId")), selectedTickets(request),
                                user.getUserId(), user.getFullName());
                // Lượt in mới thay lượt đang chờ (nếu có); chưa ghi DB cho tới khi nhân viên xác nhận
                session.setAttribute(SESSION_PENDING, job);
                session.setAttribute(SESSION_PENDING_TOKEN, UUID.randomUUID().toString());
                session.setAttribute(SESSION_AUTO_PRINT, Boolean.TRUE);
                session.setAttribute(SESSION_BOOKING, job.getBookingId());
            }
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERROR, e.getErrors().get("general"));
        } catch (SQLException e) {
            handleSystemError(response, e);
            return;
        }
        Object last = session.getAttribute(SESSION_BOOKING);
        response.sendRedirect(request.getContextPath() + "/staff/ticket-print"
                + (last instanceof Integer id ? "?booking=" + id : ""));
    }

    // Sau hộp thoại in: nhân viên xác nhận vé đã ra giấy (ghi lượt in vào DB, ghi nhật ký) hoặc chưa in được (bỏ lượt
    // in, không ghi gì). Chỉ lượt in đang chờ của chính phiên này, đúng token mới xử lý được.
    private void finishPending(HttpServletRequest request, HttpSession session, User user, boolean confirm)
            throws SQLException, ValidationException {
        Object pending = session.getAttribute(SESSION_PENDING);
        Object token = session.getAttribute(SESSION_PENDING_TOKEN);
        if (!(pending instanceof PrintBooking job) || token == null
                || !token.equals(request.getParameter("printToken"))) {
            throw new ValidationException(java.util.Map.of("general", TicketPrintService.PRINT_EXPIRED));
        }
        session.removeAttribute(SESSION_PENDING);
        session.removeAttribute(SESSION_PENDING_TOKEN);
        session.removeAttribute(SESSION_AUTO_PRINT);
        session.setAttribute(SESSION_BOOKING, job.getBookingId());
        if (!confirm) {
            session.setAttribute(FLASH_SUCCESS, printService.cancelledMessage());
            return;
        }
        printService.confirmPrint(job, user.getUserId(), user.getFullName());
        PrintTicket first = job.getTickets().get(0);
        if (first.getPrintReason() != null) {
            ActivityLogger.info(request, LogEventType.OPERATION, "In lại vé giấy", TARGET, job.getBookingId(),
                    "Vé " + first.getTicketCode() + " (ghế " + first.getSeatLabel() + "), lần in lại "
                            + first.getReprintNo() + ", lý do: " + first.getPrintReason());
            session.setAttribute(FLASH_SUCCESS, printService.reprintedMessage(first.getTicketCode()));
        } else {
            ActivityLogger.info(request, LogEventType.OPERATION, "In vé giấy", TARGET, job.getBookingId(),
                    "Đơn #" + job.getBookingId() + ": in " + job.getTickets().size() + " vé ("
                            + job.getTickets().stream().map(t -> t.getTicketCode() + " ghế " + t.getSeatLabel())
                            .collect(Collectors.joining(", ")) + ")"
                            + (job.isConcessionSlip() ? " kèm phiếu nhận bắp nước" : ""));
            session.setAttribute(FLASH_SUCCESS, printService.printedMessage(job.getTickets().size()));
        }
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
