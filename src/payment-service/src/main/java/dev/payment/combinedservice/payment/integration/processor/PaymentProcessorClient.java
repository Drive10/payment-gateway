package dev.payment.combinedservice.payment.integration.processor;

import dev.payment.combinedservice.payment.domain.Payment;
import dev.payment.combinedservice.payment.domain.enums.TransactionMode;
import dev.payment.combinedservice.payment.dto.request.CapturePaymentRequest;

public interface PaymentProcessorClient {
    PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode);
    PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode);
    boolean verifyOtp(Payment payment, String otp);
}
