package vn.edu.fpt.controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vn.edu.fpt.model.LogEventType;
import vn.edu.fpt.service.ActivityLogger;
import vn.edu.fpt.service.ValidationException;
import vn.edu.fpt.service.payment.PaymentGatewayException;
import vn.edu.fpt.service.payment.PaymentService;

@WebServlet("/api/v1/commerce/payment/webhook/vnpay")
public class VnPayWebhookServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(VnPayWebhookServlet.class.getName());
    private final PaymentService paymentService = new PaymentService();

    // VNPay gọi IPN bằng GET (tài liệu Thanh toán PAY 2.1.0: "Phương thức: GET"); POST vẫn nhận để tương thích
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleCallback(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleCallback(request, response);
    }

    private void handleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        Map<String, String> parameters = callbackParameters(request.getParameterMap());
        if (parameters == null) {
            writeResponse(response, "97", "Invalid callback parameters");
            return;
        }
        try {
            boolean accepted = paymentService.processVnPayCallback(parameters);
            logCallback(request, parameters, accepted);
            writeResponse(response, accepted ? "00" : "01", accepted ? "Success" : "Transaction not found");
        } catch (PaymentGatewayException e) {
            LOGGER.log(Level.WARNING, "VNPay callback rejected", e);
            ActivityLogger.warning(request, LogEventType.BOOKING, "Callback VNPay không hợp lệ", "Payment",
                    parameters.get("vnp_TxnRef"), e.getMessage());
            writeResponse(response, "97", "Checksum or callback validation failed");
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "VNPay callback could not settle the booking", e);
            writeResponse(response, "02", "Transaction data is invalid");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while processing VNPay callback", e);
            writeResponse(response, "99", "System error");
        }
    }

    // UC-32: chỉ ghi mã giao dịch, số tiền và mã kết quả; không ghi chữ ký hay thông tin thẻ (BR-06)
    private void logCallback(HttpServletRequest request, Map<String, String> parameters, boolean accepted) {
        String txnRef = parameters.get("vnp_TxnRef");
        String responseCode = parameters.get("vnp_ResponseCode");
        String description = "Số tiền " + parameters.get("vnp_Amount") + " (x100 VND), mã kết quả " + responseCode
                + ", mã GD VNPay " + parameters.get("vnp_TransactionNo");
        if (!accepted) {
            ActivityLogger.warning(request, LogEventType.BOOKING, "Callback VNPay không tìm thấy giao dịch", "Payment",
                    txnRef, description);
        } else if ("00".equals(responseCode) && "00".equals(parameters.get("vnp_TransactionStatus"))) {
            // Cùng điều kiện thành công với VnPayGateway; VNPay có thể gửi lại IPN nên chỉ ghi một lần mỗi giao dịch
            ActivityLogger.infoOnce(request, LogEventType.BOOKING, "Thanh toán VNPay thành công", "Payment", txnRef,
                    description);
        } else {
            ActivityLogger.warning(request, LogEventType.BOOKING, "Thanh toán VNPay thất bại", "Payment", txnRef,
                    description);
        }
    }

    private Map<String, String> callbackParameters(Map<String, String[]> rawParameters) {
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

    private void writeResponse(HttpServletResponse response, String code, String message) throws IOException {
        response.getWriter().printf("{\"RspCode\":\"%s\",\"Message\":\"%s\"}", code, message);
    }
}
