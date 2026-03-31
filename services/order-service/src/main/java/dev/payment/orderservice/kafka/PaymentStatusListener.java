package dev.payment.orderservice.kafka;

import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentStatusListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusListener.class);

    private final OrderService orderService;

    public PaymentStatusListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-events:payment.status.updated}", groupId = "order-service")
    public void onPaymentStatusUpdated(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            if (!"PAYMENT_STATUS_UPDATED".equals(eventType)) {
                return;
            }

            UUID orderId = parseUUID(event.get("orderId"));
            String paymentStatus = (String) event.get("paymentStatus");

            if (orderId == null) {
                log.warn("Received payment event without orderId");
                return;
            }

            OrderStatus newOrderStatus = mapPaymentStatusToOrderStatus(paymentStatus);
            if (newOrderStatus != null) {
                orderService.updateOrderStatus(orderId, newOrderStatus);
                log.info("Updated order {} status to {}", orderId, newOrderStatus);
            }
        } catch (Exception e) {
            log.error("Error processing payment status event", e);
        }
    }

    private UUID parseUUID(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private OrderStatus mapPaymentStatusToOrderStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return null;
        }
        return switch (paymentStatus.toUpperCase()) {
            case "SUCCESS", "CAPTURED", "COMPLETED" -> OrderStatus.PAID;
            case "FAILED" -> OrderStatus.FAILED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            case "EXPIRED" -> OrderStatus.EXPIRED;
            default -> null;
        };
    }
}
