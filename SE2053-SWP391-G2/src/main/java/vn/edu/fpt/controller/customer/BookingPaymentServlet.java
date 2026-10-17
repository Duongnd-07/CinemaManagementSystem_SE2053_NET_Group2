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
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.BookingSummary;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

// Sau UC-13 bước 9: đơn chờ thanh toán. Cổng thanh toán thật thuộc UC-14 (thành viên khác làm);
// trang này chỉ mô phỏng kết quả thanh toán thành công để chạy trọn luồng đặt vé, kèm AF2 hủy đơn.
@WebServlet(BookingFlow.PAYMENT_PATH)
public class BookingPaymentServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingPaymentServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/booking-payment.jsp";
    private static final String ACTION_PAY = "pay";
    private static final String ACTION_CANCEL = "cancel";

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        BookingFlow.moveFlashToRequest(session, request);
        int bookingId = BookingFlow.parseInt(request.getParameter("id"));
        try {
            BookingSummary summary = bookingService.loadBooking(BookingFlow.currentUser(session), bookingId);
            request.setAttribute("summary", summary);
            request.setAttribute("csrfToken", CsrfToken.get(session));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (ValidationException e) {
            session.setAttribute(BookingFlow.FLASH_ERROR, e.getErrors().values().iterator().next());
            response.sendRedirect(request.getContextPath() + "/movies");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải đơn đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!CsrfToken.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        HttpSession session = request.getSession();
        User user = BookingFlow.currentUser(session);
        int bookingId = BookingFlow.parseInt(request.getParameter("id"));
        String action = request.getParameter("action");
        try {
            if (ACTION_PAY.equals(action)) {
                int pointBalance = bookingService.payDemo(user, bookingId);
                // Header hiển thị số điểm từ user trong session
                user.setPointBalance(pointBalance);
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_PAID);
            } else if (ACTION_CANCEL.equals(action)) {
                bookingService.cancelBooking(user, bookingId);
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_CANCELLED);
            }
        } catch (ValidationException e) {
            session.setAttribute(BookingFlow.FLASH_ERROR, e.getErrors().values().iterator().next());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi xử lý thanh toán/hủy đơn đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
            return;
        }
        response.sendRedirect(BookingFlow.paymentUrl(request, bookingId));
    }
}
