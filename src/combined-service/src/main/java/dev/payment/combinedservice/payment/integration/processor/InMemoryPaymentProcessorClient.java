package dev.payment.combinedservice.payment.integration.processor;

import dev.payment.combinedservice.payment.domain.Payment;
import dev.payment.combinedservice.payment.domain.enums.TransactionMode;
import dev.payment.combinedservice.payment.dto.request.CapturePaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
public class InMemoryPaymentProcessorClient implements PaymentProcessorClient {

    @Value("${application.payment.simulator-url:https://simulator.test/checkout/}")
    private String simulatorUrl;

    @Value("${application.payment.checkout-url:https://checkout.fintech.local/pay/}")
    private String checkoutUrl;

    @Override
    public PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode) {
        String prefix = mode == TransactionMode.TEST ? "test_order_" : "prod_order_";
        String providerOrderId = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        String url = mode == TransactionMode.TEST ? simulatorUrl : checkoutUrl;
        String checkoutUrl = url + providerOrderId;
        return new PaymentProcessorIntentResponse(providerOrderId, checkoutUrl, mode == TransactionMode.TEST);
    }

    @Override
    public PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode) {
        String providerPaymentId = request.providerPaymentId();
        if (providerPaymentId == null || providerPaymentId.isBlank()) {
            providerPaymentId = (mode == TransactionMode.TEST ? "test_pay_" : "live_pay_")
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        }
        String providerSignature = request.providerSignature();
        if (providerSignature == null || providerSignature.isBlank()) {
            providerSignature = (mode == TransactionMode.TEST ? "test_sig_" : "live_sig_")
                    + payment.getProviderOrderId().substring(Math.max(0, payment.getProviderOrderId().length() - 12));
        }
        String providerReference = (mode == TransactionMode.TEST ? "test_txn_" : "live_txn_")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        return new PaymentProcessorCaptureResponse(providerPaymentId, providerSignature, providerReference, mode == TransactionMode.TEST);
    }

    @Override
    public boolean verifyOtp(Payment payment, String otp) {
        if (payment.getTransactionMode() == TransactionMode.TEST) {
            return "123456".equals(otp);
        }
        
        return otp != null && otp.length() >= 4 && !otp.isBlank();
    }
}
