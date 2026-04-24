package dev.payment.paymentservice.order.service;

import dev.payment.paymentservice.order.dto.InitiatePaymentRequest;
import dev.payment.paymentservice.order.dto.InitiatePaymentResponse;
import dev.payment.paymentservice.order.dto.OrderResponse;
import dev.payment.paymentservice.payment.domain.User;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.payment.dto.response.PaymentResponse;
import dev.payment.paymentservice.payment.service.AuthService;
import dev.payment.paymentservice.payment.service.PaymentService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentOrchestrator {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AuthService authService;

    public PaymentOrchestrator(
            OrderService orderService,
            PaymentService paymentService,
            AuthService authService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.authService = authService;
    }

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        OrderResponse order = orderService.getOrder(request.orderId());
        UUID merchantId = resolveMerchantId(order);

        CreatePaymentRequest createPaymentRequest = CreatePaymentRequest.createLegacy(
                request.orderId(),
                merchantId,
                PaymentMethod.CARD,
                "RAZORPAY_SIMULATOR",
                TransactionMode.TEST,
                "Initiated from order orchestrator"
        );

        User actor = authService.getCurrentUser("system@payflow.dev");
        String idempotencyKey = "order-init-" + request.orderId() + "-" + System.currentTimeMillis();
        PaymentResponse payment = paymentService.createPayment(createPaymentRequest, idempotencyKey, actor);

        return new InitiatePaymentResponse(
                payment.id(),
                request.orderId(),
                payment.status(),
                payment.checkoutUrl()
        );
    }

    private UUID resolveMerchantId(OrderResponse order) {
        if (order.merchantId() != null && !order.merchantId().isBlank()) {
            try {
                return UUID.fromString(order.merchantId());
            } catch (IllegalArgumentException ignored) {
                // Fall back to order owner when merchant id is not UUID formatted.
            }
        }
        return order.userId();
    }
}
