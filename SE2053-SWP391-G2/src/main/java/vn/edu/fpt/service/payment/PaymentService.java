package vn.edu.fpt.service.payment;

import java.sql.SQLException;
import java.util.LinkedHashMap;
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

    // Return URL: VNPay gửi kèm đúng bộ tham số có chữ ký HMAC như IPN, nên xác minh chữ ký rồi chốt đơn luôn tại đây.
    // Nhờ vậy chạy local (VNPay không gọi IPN vào localhost được) vẫn thanh toán được mà không cần tunnel.
    // Chốt đơn idempotent: IPN tới trước/sau (khi deploy có URL công khai) chỉ được xử lý một lần.
    public VerifiedPayment processVnPayReturn(Map<String, String> returnParameters)
            throws PaymentGatewayException, SQLException, ValidationException {
        VerifiedPayment payment = gateway.verifyCallback(returnParameters);
        bookingService.applyVerifiedVnPayPayment(payment);
        return payment;
    }

    // Tham số VNPay gửi về (IPN/Return): mỗi khóa đúng một giá trị, giới hạn số lượng và độ dài; sai thì trả null
    public static Map<String, String> callbackParameters(Map<String, String[]> rawParameters) {
        if (rawParameters == null || rawParameters.isEmpty() || rawParameters.size() > 50) {
            return null;
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : rawParameters.entrySet()) {
            String[] values = entry.getValue();
            if (entry.getKey() == null || entry.getKey().length() > 100 || values == null || values.length != 1
                    || values[0] == null || values[0].length() > 2048) {
                return null;
            }
            parameters.put(entry.getKey(), values[0]);
        }
        return parameters;
    }
}
