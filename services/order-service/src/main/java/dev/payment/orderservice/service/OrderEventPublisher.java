package dev.payment.orderservice.service;

import dev.payment.orderservice.entity.Order;
import dev.payment.orderservice.entity.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topic.order-events}")
    private String orderEventsTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("eventId", UUID.randomUUID());
        event.put("eventVersion", "1.0");
        event.put("eventType", "ORDER_CREATED");
        event.put("orderId", order.getId());
        event.put("userId", order.getUserId());
        event.put("amount", order.getAmount());
        event.put("currency", order.getCurrency());
        event.put("status", order.getStatus().name());
        event.put("occurredAt", Instant.now());

        sendEvent(order.getId().toString(), event);
    }

    public void publishOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        Map<String, Object> event = new java.util.HashMap<>();
        event.put("eventId", UUID.randomUUID());
        event.put("eventVersion", "1.0");
        event.put("eventType", "ORDER_STATUS_CHANGED");
        event.put("orderId", order.getId());
        event.put("userId", order.getUserId());
        event.put("oldStatus", oldStatus.name());
        event.put("newStatus", newStatus.name());
        event.put("amount", order.getAmount());
        event.put("currency", order.getCurrency());
        event.put("occurredAt", Instant.now());

        sendEvent(order.getId().toString(), event);
    }

    private void sendEvent(String key, Map<String, Object> event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderEventsTopic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send order event: {}", event, ex);
            } else {
                log.info("Order event sent successfully: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
