package dev.payment.paymentservice.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.order.entity.OrderStatus;
import dev.payment.paymentservice.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Component("orderPaymentStatusListener")
@Profile("!local")
public class PaymentStatusListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusListener.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentStatusListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-events:payment.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentStatus(@Payload String message,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     Acknowledgment ack) {
        try {
            log.info("Received payment event from {}: {}", topic, message);
            
            PaymentEventMessage event = objectMapper.readValue(message, PaymentEventMessage.class);
            
            if (event.orderId() == null) {
                log.warn("Payment event missing order ID, skipping: {}", event.eventId());
                ack.acknowledge();
                return;
            }
            
            OrderStatus newStatus = mapPaymentStatusToOrderStatus(event);
            if (newStatus != null) {
                orderService.updateOrderStatus(event.orderId(), newStatus);
                log.info("Updated order {} status to {} based on payment event", event.orderId(), newStatus);
            }
            
            ack.acknowledge();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment status message: {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment status update. Message: {}. Error: {}", message, e.getMessage(), e);
        }
    }

    private OrderStatus mapPaymentStatusToOrderStatus(PaymentEventMessage event) {
        String paymentStatus = event.paymentStatus();
        String eventType = event.eventType();
        
        if (eventType == null || paymentStatus == null) {
            return null;
        }
        
        return switch (eventType) {
            case "payment.captured", "payment.completed" -> {
                if ("CAPTURED".equals(paymentStatus) || "COMPLETED".equals(paymentStatus)) {
                    yield OrderStatus.PAID;
                }
                yield null;
            }
            case "payment.failed" -> {
                if ("FAILED".equals(paymentStatus)) {
                    yield OrderStatus.FAILED;
                }
                yield null;
            }
            case "payment.refunded" -> {
                if ("REFUNDED".equals(paymentStatus)) {
                    yield OrderStatus.REFUNDED;
                }
                yield null;
            }
            default -> null;
        };
    }
}
