package dev.payment.paymentservice.payment.integration.processor;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.dto.request.CapturePaymentRequest;

public interface PaymentProcessorClient {
    PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode);
    PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode);
    boolean verifyOtp(Payment payment, String otp);
}
