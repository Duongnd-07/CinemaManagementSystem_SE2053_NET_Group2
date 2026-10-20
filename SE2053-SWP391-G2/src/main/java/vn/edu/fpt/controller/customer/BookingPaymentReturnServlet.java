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
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.service.payment.PaymentService;
import vn.edu.fpt.util.Messages;

@WebServlet("/customer/booking/payment/return")
public class BookingPaymentReturnServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingPaymentReturnServlet.class.getName());
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String reference = request.getParameter("vnp_TxnRef");
        User user = BookingFlow.currentUser(session);
        try {
            Integer bookingId = paymentService.findBookingIdForReturn(user, reference);
            if (bookingId == null) {
                session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_NOT_FOUND);
                response.sendRedirect(request.getContextPath() + "/movies");
                return;
            }
            session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_VNPAY_RETURN_PENDING);
            response.sendRedirect(BookingFlow.paymentUrl(request, bookingId));
        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tra cứu kết quả chuyển hướng VNPay", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }
}
