package vn.edu.fpt.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.BookingStatus;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.service.payment.PaymentGatewayException;
import vn.edu.fpt.service.payment.PaymentService;
import vn.edu.fpt.service.payment.VerifiedPayment;
import vn.edu.fpt.util.Messages;

// UC-14: VNPay chuyển trình duyệt của khách về đây sau khi thanh toán. Tham số có chữ ký HMAC như IPN nên đơn
// được chốt ngay tại đây (chạy local không cần IPN/tunnel); IPN tới sau chỉ được xử lý một lần.
@WebServlet("/customer/booking/payment/return")
public class BookingPaymentReturnServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BookingPaymentReturnServlet.class.getName());
    private final PaymentService paymentService = new PaymentService();
    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String reference = request.getParameter("vnp_TxnRef");
        User user = BookingFlow.currentUser(session);
        try {
            // Chốt đơn trước khi tra cứu theo phiên đăng nhập: chữ ký đã đúng thì kết quả thanh toán không phụ thuộc
            // việc khách còn phiên đăng nhập hay không
            VerifiedPayment payment = settle(request);
            Integer bookingId = paymentService.findBookingIdForReturn(user, reference);
            if (bookingId == null) {
                session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_NOT_FOUND);
                response.sendRedirect(request.getContextPath() + "/movies");
                return;
            }
            if (payment == null) {
                session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_VNPAY_INVALID_RETURN);
            } else if (!payment.isSuccessful()) {
                session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_VNPAY_FAILED);
            } else if (BookingStatus.PAID.equals(
                    bookingService.loadBooking(user, bookingId).getBooking().getStatus())) {
                session.setAttribute(BookingFlow.FLASH_SUCCESS, Messages.BOOKING_PAID);
            } else {
                session.setAttribute(BookingFlow.FLASH_ERROR, Messages.BOOKING_VNPAY_REVIEW);
            }
            response.sendRedirect(BookingFlow.paymentUrl(request, bookingId));
        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi xử lý kết quả chuyển hướng VNPay", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.SYSTEM_ERROR);
        }
    }

    // null khi tham số sai định dạng hoặc chữ ký không hợp lệ (không chốt gì cả)
    private VerifiedPayment settle(HttpServletRequest request) throws SQLException {
        Map<String, String> parameters = PaymentService.callbackParameters(request.getParameterMap());
        if (parameters == null) {
            return null;
        }
        String txnRef = parameters.get("vnp_TxnRef");
        try {
            VerifiedPayment payment = paymentService.processVnPayReturn(parameters);
            String description = "Qua Return URL; số tiền " + parameters.get("vnp_Amount") + " (x100 VND), mã kết quả "
                    + payment.getResponseCode() + ", mã GD VNPay " + payment.getProviderTransactionId();
            if (payment.isSuccessful()) {
                // Cùng hành động với IPN (VnPayWebhookServlet) nên mỗi giao dịch chỉ ghi một dòng dù đến từ đường nào
                ActivityLogger.infoOnce(request, LogEventType.BOOKING, "Thanh toán VNPay thành công", "Payment",
                        txnRef, description);
            } else {
                ActivityLogger.warning(request, LogEventType.BOOKING, "Thanh toán VNPay thất bại", "Payment", txnRef,
                        description);
            }
            return payment;
        } catch (PaymentGatewayException e) {
            LOGGER.log(Level.WARNING, "VNPay return rejected", e);
            ActivityLogger.warning(request, LogEventType.BOOKING, "Return VNPay không hợp lệ", "Payment", txnRef,
                    e.getMessage());
            return null;
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "VNPay return could not settle the booking", e);
            return null;
        }
    }
}
