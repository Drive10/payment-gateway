package dev.payment.orderservice.kafka;

import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class PaymentStatusListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusListener.class);

    private final OrderService orderService;

    public PaymentStatusListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-events:payment.events}", groupId = "order-service")
    public void onPaymentEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            if (eventType == null) {
                log.warn("Received payment event without eventType");
                return;
            }

            UUID orderId = parseUUID(event.get("orderId"));
            if (orderId == null) {
                log.warn("Received payment event without orderId: {}", eventType);
                return;
            }

            OrderStatus newOrderStatus = mapEventTypeToOrderStatus(eventType);
            if (newOrderStatus != null) {
                orderService.updateOrderStatus(orderId, newOrderStatus);
                log.info("Updated order {} status to {} from event {}", orderId, newOrderStatus, eventType);
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

    private OrderStatus mapEventTypeToOrderStatus(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "PAYMENT.CAPTURED", "PAYMENT.WEBHOOK.CAPTURED" -> OrderStatus.PAID;
            case "PAYMENT.FAILED", "PAYMENT.WEBHOOK.FAILED" -> OrderStatus.FAILED;
            case "PAYMENT.REFUNDED", "PAYMENT.WEBHOOK.REFUND_PROCESSED" -> OrderStatus.REFUNDED;
            default -> null;
        };
    }
}
