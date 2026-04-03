package dev.payment.orderservice.service;

import dev.payment.orderservice.dto.InitiatePaymentRequest;
import dev.payment.orderservice.dto.InitiatePaymentResponse;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.exception.OrderException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentOrchestrator {

    private WebClient webClient;
    private final OrderService orderService;

    @Value("${application.payment-service.url}")
    private String paymentServiceUrl;

    public PaymentOrchestrator(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder().baseUrl(paymentServiceUrl).build();
    }

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        OrderResponse order = orderService.getOrder(request.orderId());

        if (!order.amount().equals(request.amount()) || !order.currency().equals(request.currency())) {
            throw new OrderException("Order amount or currency mismatch");
        }

        if (order.status() != OrderStatus.PENDING) {
            throw new OrderException("Order is not in PENDING status");
        }

        try {
            InitiatePaymentResponse response = webClient.post()
                    .uri("/payments")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InitiatePaymentResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(e -> Mono.error(new OrderException("Failed to initiate payment: " + e.getMessage())))
                    .block();

            return response;
        } catch (Exception e) {
            throw new OrderException("Payment service unavailable: " + e.getMessage());
        }
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
}
