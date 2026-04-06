package dev.payment.orderservice.kafka;

import dev.payment.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentStatusListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusListener.class);

    private final OrderService orderService;

    public PaymentStatusListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-status:payment.status}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentStatus(@Payload String message,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     Acknowledgment ack) {
        try {
            log.info("Received payment status update from {}: {}", topic, message);
            orderService.updateOrderStatus(UUID.fromString(message), dev.payment.orderservice.entity.OrderStatus.PAID);
            ack.acknowledge();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment status message: {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process payment status update. Message: {}. Error: {}", message, e.getMessage(), e);
        }
    }
}
