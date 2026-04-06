package dev.payment.paymentservice.integration.processor;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;

public interface PaymentProcessorClient {
    PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode);
    PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode);
}
