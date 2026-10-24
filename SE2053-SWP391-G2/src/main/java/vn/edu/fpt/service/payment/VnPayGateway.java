package vn.edu.fpt.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import vn.edu.fpt.util.AppConfig;

public class VnPayGateway implements PaymentGateway {
    private static final String HASH_ALGORITHM = "HmacSHA512";
    private static final String HASH_TYPE = "HmacSHA512";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VN_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter VNPAY_DATE =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    private static final int MAX_URL_LENGTH = 2048;

    @Override
    public PaymentSession createPayment(PaymentRequest request) throws PaymentGatewayException {
        validateRequest(request);
        String terminalCode = requiredConfig("payment.vnpay.tmn-code");
        if (!terminalCode.matches("[A-Za-z0-9]{1,20}")) {
            throw new PaymentGatewayException("Invalid VNPay terminal code configuration.");
        }
        String paymentUrl = requireHttpsUrl(requiredConfig("payment.vnpay.payment-url"), "payment URL");
        String secret = requiredConfig("payment.vnpay.hash-secret");
        int expireMinutes = AppConfig.getInt("payment.vnpay.expire-minutes", 10);
        if (expireMinutes < 1 || expireMinutes > 15) {
            throw new PaymentGatewayException("VNPay expiry must be between 1 and 15 minutes.");
        }

        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        LocalDateTime expiresAt = request.getExpiresAt();
        LocalDateTime maximumExpiry = now.plusMinutes(expireMinutes);
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new PaymentGatewayException("The booking hold expired before VNPay payment was created.");
        }
        if (expiresAt == null || expiresAt.isAfter(maximumExpiry)) {
            expiresAt = maximumExpiry;
        }
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", terminalCode);
        parameters.put("vnp_Amount", amountInMinorUnits(request.getAmount()));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", request.getTransactionReference());
        parameters.put("vnp_OrderInfo", "Thanh toan don hang " + request.getBookingId());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", requireReturnUrl(request.getReturnUrl()));
        parameters.put("vnp_IpAddr", request.getClientIpAddress());
        parameters.put("vnp_CreateDate", VN_TIME.format(now));
        parameters.put("vnp_ExpireDate", VN_TIME.format(expiresAt));
        String signedData = canonicalQuery(parameters);
        String signature = hmac(secret, signedData);
        // 2.1.0 chỉ gửi vnp_SecureHash (không có vnp_SecureHashType trong danh sách tham số tạo URL)
        String url = paymentUrl + "?" + signedData + "&vnp_SecureHash=" + signature;
        if (url.length() > MAX_URL_LENGTH) {
            throw new PaymentGatewayException("The VNPay payment URL exceeds the supported length.");
        }

        PaymentSession session = new PaymentSession();
        session.setProviderTransactionId(request.getTransactionReference());
        session.setPaymentUrl(url);
        session.setExpiresAt(expiresAt);
        return session;
    }

    @Override
    public VerifiedPayment verifyCallback(Map<String, String> callbackParameters) throws PaymentGatewayException {
        if (callbackParameters == null || callbackParameters.size() > 50) {
            throw new PaymentGatewayException("Invalid VNPay callback parameters.");
        }
        String receivedHash = callbackParameters.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.length() != 128) {
            throw new PaymentGatewayException("Missing or malformed VNPay signature.");
        }
        String secret = requiredConfig("payment.vnpay.hash-secret");
        Map<String, String> signedParameters = new TreeMap<>();
        for (Map.Entry<String, String> entry : callbackParameters.entrySet()) {
            String key = entry.getKey();
            if (!"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)) {
                signedParameters.put(key, entry.getValue());
            }
        }
        String expectedHash = hmac(secret, canonicalQuery(signedParameters));
        if (!MessageDigest.isEqual(expectedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                receivedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw new PaymentGatewayException("VNPay callback signature verification failed.");
        }
        // Bản 2.1.0 không trả vnp_SecureHashType (chữ ký luôn là HMAC-SHA512); chỉ từ chối khi có mà khác thuật toán
        String hashType = callbackParameters.get("vnp_SecureHashType");
        if (hashType != null && !HASH_TYPE.equalsIgnoreCase(hashType)) {
            throw new PaymentGatewayException("Unsupported VNPay signature algorithm.");
        }

        String reference = requiredCallbackValue(callbackParameters, "vnp_TxnRef", 100);
        String responseCode = requiredCallbackValue(callbackParameters, "vnp_ResponseCode", 2);
        String transactionStatus = requiredCallbackValue(callbackParameters, "vnp_TransactionStatus", 2);
        String amountValue = requiredCallbackValue(callbackParameters, "vnp_Amount", 20);
        if (!responseCode.matches("[0-9]{2}") || !transactionStatus.matches("[0-9]{2}")
                || !amountValue.matches("[0-9]{1,20}")) {
            throw new PaymentGatewayException("Malformed VNPay response values.");
        }
        String terminalCode = requiredCallbackValue(callbackParameters, "vnp_TmnCode", 20);
        if (!terminalCode.equals(requiredConfig("payment.vnpay.tmn-code"))) {
            throw new PaymentGatewayException("VNPay terminal code does not match this merchant.");
        }
        // IPN/Return 2.1.0 không có vnp_CurrCode (luôn là VND như lúc tạo URL); chỉ từ chối khi có mà khác VND
        String currency = callbackParameters.get("vnp_CurrCode");
        if (currency != null && !"VND".equals(currency)) {
            throw new PaymentGatewayException("Unsupported VNPay currency.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException e) {
            throw new PaymentGatewayException("Malformed VNPay amount.", e);
        }
        String transactionId = callbackParameters.getOrDefault("vnp_TransactionNo", "");
        if (transactionId.length() > 100) {
            throw new PaymentGatewayException("Malformed VNPay transaction identifier.");
        }
        boolean successful = "00".equals(responseCode) && "00".equals(transactionStatus);
        // vnp_PayDate là tham số tùy chọn theo tài liệu; thiếu thì lấy thời điểm nhận IPN đã xác minh chữ ký
        String payDate = callbackParameters.get("vnp_PayDate");
        LocalDateTime paidAt = !successful ? null
                : payDate == null || payDate.isBlank() ? LocalDateTime.now(VN_ZONE)
                : parsePaidAt(requiredCallbackValue(callbackParameters, "vnp_PayDate", 14));
        return new VerifiedPayment(reference, transactionId, successful, amount, paidAt, responseCode);
    }

    private void validateRequest(PaymentRequest request) throws PaymentGatewayException {
        if (request == null || request.getBookingId() <= 0 || request.getCustomerId() <= 0
                || request.getAmount() == null || request.getAmount().signum() <= 0
                || request.getTransactionReference() == null
                || !request.getTransactionReference().matches("[A-Za-z0-9_-]{1,100}")
                || request.getClientIpAddress() == null || request.getClientIpAddress().isBlank()
                || request.getClientIpAddress().length() > 45) {
            throw new PaymentGatewayException("Invalid payment request.");
        }
        requireReturnUrl(request.getReturnUrl());
        // IPN URL đăng ký trên trang Merchant/SIT của VNPay, không gửi kèm URL thanh toán; chạy local không có IPN
        // (đơn được chốt ở Return URL) nên chỉ kiểm tra khi có cấu hình
        if (request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank()) {
            requireHttpsUrl(request.getCallbackUrl(), "callback URL");
        }
        amountInMinorUnits(request.getAmount());
    }

    // Return URL chỉ là nơi VNPay chuyển TRÌNH DUYỆT của khách về, nên chạy local được với http://localhost
    // (không cần tunnel); host khác bắt buộc HTTPS
    private String requireReturnUrl(String value) throws PaymentGatewayException {
        if (value != null && !value.isBlank()) {
            try {
                URI uri = URI.create(value);
                String host = uri.getHost();
                if ("http".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null && uri.getFragment() == null
                        && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host))) {
                    return uri.toASCIIString();
                }
            } catch (IllegalArgumentException e) {
                throw new PaymentGatewayException("Invalid return URL.");
            }
        }
        return requireHttpsUrl(value, "return URL");
    }

    private String amountInMinorUnits(BigDecimal amount) throws PaymentGatewayException {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY)
                    .multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString();
        } catch (ArithmeticException e) {
            throw new PaymentGatewayException("VNPay requires an integer VND amount.", e);
        }
    }

    private String requiredConfig(String key) throws PaymentGatewayException {
        String value = AppConfig.get(key, "");
        if (value.isBlank()) {
            throw new PaymentGatewayException("Missing required configuration: " + key);
        }
        return value;
    }

    private String requireHttpsUrl(String value, String label) throws PaymentGatewayException {
        try {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null
                    || ("payment URL".equals(label) && uri.getRawQuery() != null)) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException e) {
            throw new PaymentGatewayException("Invalid HTTPS " + label + ".");
        }
    }

    private String requiredCallbackValue(Map<String, String> parameters, String key, int maxLength)
            throws PaymentGatewayException {
        String value = parameters.get(key);
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new PaymentGatewayException("Missing or malformed VNPay parameter: " + key);
        }
        return value;
    }

    private LocalDateTime parsePaidAt(String value) throws PaymentGatewayException {
        try {
            return LocalDateTime.parse(value, VNPAY_DATE);
        } catch (RuntimeException e) {
            throw new PaymentGatewayException("Malformed VNPay payment time.", e);
        }
    }

    private String canonicalQuery(Map<String, String> parameters) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return query.toString();
    }

    private String hmac(String secret, String data) throws PaymentGatewayException {
        try {
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HASH_ALGORITHM));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new PaymentGatewayException("Cannot compute VNPay signature.", e);
        }
    }
}
