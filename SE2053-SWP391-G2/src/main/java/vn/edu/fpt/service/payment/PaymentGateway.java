package vn.edu.fpt.service.payment;

import java.util.Map;

public interface PaymentGateway {
    PaymentSession createPayment(PaymentRequest request) throws PaymentGatewayException;

    VerifiedPayment verifyCallback(Map<String, String> callbackParameters) throws PaymentGatewayException;
}
