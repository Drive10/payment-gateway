package dev.payment.orderservice.service;

import dev.payment.orderservice.dto.InitiatePaymentRequest;
import dev.payment.orderservice.dto.InitiatePaymentResponse;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.exception.OrderException;
import dev.payment.orderservice.config.ServiceConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

    private final WebClient webClient;
    private final OrderService orderService;
    private final long requestTimeoutMs;

    public PaymentOrchestrator(
            OrderService orderService,
            WebClient.Builder webClientBuilder,
            ServiceConfig serviceConfig) {
        this.orderService = orderService;
        this.webClient = webClientBuilder.baseUrl(serviceConfig.getPaymentService().getUrl()).build();
        this.requestTimeoutMs = serviceConfig.getPaymentService().getRequestTimeoutMs();
    }

    @Retry(name = "paymentService")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "initiatePaymentFallback")
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        try {
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", request.orderId().toString());
            paymentRequest.put("amount", request.amount().toPlainString());
            paymentRequest.put("currency", request.currency());
            paymentRequest.put("returnUrl", request.returnUrl());
            paymentRequest.put("cardToken", "token_" + System.currentTimeMillis());
            paymentRequest.put("merchantId", "system-merchant");
            paymentRequest.put("idempotencyKey", "idem_" + System.currentTimeMillis());
            paymentRequest.put("paymentMethod", "CARD");

            InitiatePaymentResponse response = webClient.post()
                    .uri("/payments/initiate")
                    .bodyValue(paymentRequest)
                    .retrieve()
                    .bodyToMono(InitiatePaymentResponse.class)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .onErrorResume(e -> Mono.error(new OrderException("Failed to initiate payment: " + e.getMessage())))
                    .block();

            return response;
        } catch (Exception e) {
            log.error("Payment orchestration failed: {}", e.getMessage(), e);
            throw new OrderException("Payment service unavailable: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private InitiatePaymentResponse initiatePaymentFallback(InitiatePaymentRequest request, Throwable throwable) {
        log.warn("Payment service fallback triggered for order {}: {}", request.orderId(), throwable.getMessage());
        throw new OrderException("Payment service is currently unavailable. Please retry shortly.");
    }

    public void handlePaymentCallback(UUID orderId, String paymentStatus) {
        OrderStatus newStatus = switch (paymentStatus.toUpperCase()) {
            case "SUCCESS", "CAPTURED" -> OrderStatus.PAID;
            case "FAILED" -> OrderStatus.FAILED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            case "EXPIRED" -> OrderStatus.EXPIRED;
            default -> null;
        };

        if (newStatus != null) {
            orderService.updateOrderStatus(orderId, newStatus);
        }
    }

    public void capturePayment(UUID orderId, String providerReference) {
        try {
            Map<String, Object> captureRequest = new HashMap<>();
            captureRequest.put("providerReference", providerReference);

            webClient.post()
                    .uri("/payments/" + getPaymentIdByOrderId(orderId) + "/capture")
                    .bodyValue(captureRequest)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Payment capture failed for order {}: {}", orderId, e.getMessage(), e);
            throw new OrderException("Failed to capture payment: " + e.getMessage());
        }
    }

    private String getPaymentIdByOrderId(UUID orderId) {
        return orderService.getOrder(orderId).id().toString();
    }
}
