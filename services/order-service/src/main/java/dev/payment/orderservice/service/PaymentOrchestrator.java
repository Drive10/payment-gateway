package dev.payment.orderservice.service;

import dev.payment.orderservice.dto.InitiatePaymentRequest;
import dev.payment.orderservice.dto.InitiatePaymentResponse;
import dev.payment.orderservice.entity.Order;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.exception.OrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
public class PaymentOrchestrator {

    private final WebClient webClient;
    private final OrderService orderService;

    @Value("${application.payment-service.url}")
    private String paymentServiceUrl;

    public PaymentOrchestrator(WebClient.Builder webClientBuilder, OrderService orderService) {
        this.webClient = webClientBuilder.baseUrl(paymentServiceUrl).build();
        this.orderService = orderService;
    }

    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        Order order = orderService.getOrder(request.orderId()).id() != null ? null : null;
        
        if (!order.getAmount().equals(request.amount()) || !order.getCurrency().equals(request.currency())) {
            throw new OrderException("Order amount or currency mismatch");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderException("Order is not in PENDING status");
        }

        try {
            InitiatePaymentResponse response = webClient.post()
                    .uri("/api/v1/payments")
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
