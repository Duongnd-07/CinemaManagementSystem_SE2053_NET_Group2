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
import vn.edu.fpt.service.payment.PaymentGatewayException;
import vn.edu.fpt.service.payment.PaymentService;
import vn.edu.fpt.util.CsrfToken;
import vn.edu.fpt.util.Messages;

@WebServlet(BookingFlow.PAYMENT_PATH)
public class BookingPaymentServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingPaymentServlet.class.getName());
    private static final String VIEW = "/WEB-INF/views/customer/booking-payment.jsp";
    private static final String ACTION_PAY = "pay";
    private static final String ACTION_CONFIRM_FREE = "confirmFree";
    private static final String ACTION_CANCEL = "cancel";

    private final BookingService bookingService = new BookingService();
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        BookingFlow.moveFlashToRequest(session, request);
        int bookingId = BookingFlow.parseInt(request.getParameter("id"));
        try {
            User user = BookingFlow.currentUser(session);
            BookingSummary summary = bookingService.loadBooking(user, bookingId);
            // VNPay chốt đơn qua IPN (không qua phiên của khách) nên làm mới số điểm hiển thị ở header tại đây
            user.setPointBalance(summary.getPointBalance());
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
                var paymentSession = paymentService.startVnPayPayment(user, bookingId, request.getRemoteAddr());
                response.sendRedirect(paymentSession.getPaymentUrl());
                return;
            } else if (ACTION_CONFIRM_FREE.equals(action)) {
                bookingService.confirmFreeBooking(user, bookingId);
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_FREE_CONFIRMED);
            } else if (ACTION_CANCEL.equals(action)) {
                bookingService.cancelBooking(user, bookingId);
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_CANCELLED);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (ValidationException e) {
            session.setAttribute(BookingFlow.FLASH_ERROR, e.getErrors().values().iterator().next());
        } catch (PaymentGatewayException e) {
            LOGGER.log(Level.SEVERE, "Không thể khởi tạo thanh toán VNPay", e);
            session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_VNPAY_START_FAILED);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi xử lý thanh toán/hủy đơn đặt vé", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
            return;
        }
        response.sendRedirect(BookingFlow.paymentUrl(request, bookingId));
    }
}
