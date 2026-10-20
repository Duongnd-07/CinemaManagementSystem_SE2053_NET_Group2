package vn.edu.fpt.service.payment;

import java.sql.SQLException;
import java.util.Map;
import vn.edu.fpt.model.User;
import vn.edu.fpt.service.BookingService;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.util.AppConfig;

public class PaymentService {
    private final PaymentGateway gateway = new VnPayGateway();
    private final BookingService bookingService = new BookingService();

    public PaymentSession startVnPayPayment(User customer, int bookingId, String clientIpAddress)
            throws SQLException, ValidationException, PaymentGatewayException {
        return bookingService.startVnPayPayment(customer, bookingId, clientIpAddress,
                AppConfig.get("payment.vnpay.return-url", ""),
                AppConfig.get("payment.vnpay.callback-url", ""), gateway);
    }

    public Integer findBookingIdForReturn(User customer, String transactionReference)
            throws SQLException, ValidationException {
        return bookingService.findBookingIdForPaymentReturn(customer, transactionReference);
    }

    public boolean processVnPayCallback(Map<String, String> callbackParameters)
            throws PaymentGatewayException, SQLException, ValidationException {
        VerifiedPayment payment = gateway.verifyCallback(callbackParameters);
        return bookingService.applyVerifiedVnPayPayment(payment);
    }
}
