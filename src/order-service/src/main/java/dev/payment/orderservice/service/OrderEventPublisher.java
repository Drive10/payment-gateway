package dev.payment.orderservice.service;

import dev.payment.orderservice.config.KafkaConfig;
import dev.payment.orderservice.entity.Order;
import dev.payment.orderservice.entity.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final int MAX_RETRIES = 3;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConfig kafkaConfig;
    private final Counter eventSuccessCounter;
    private final Counter eventFailureCounter;
    private final Counter eventDlqCounter;

    @Value("${application.kafka.topic.order-events}")
    private String orderEventsTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaConfig kafkaConfig,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaConfig = kafkaConfig;
        this.eventSuccessCounter = Counter.builder("order.event.published")
                .description("Successfully published order events")
                .register(meterRegistry);
        this.eventFailureCounter = Counter.builder("order.event.publish_failure")
                .description("Failed order event publishes")
                .register(meterRegistry);
        this.eventDlqCounter = Counter.builder("order.event.dead_letter")
                .description("Order events sent to DLQ")
                .register(meterRegistry);
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

        sendEvent(order.getId().toString(), event, 1);
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

        sendEvent(order.getId().toString(), event, 1);
    }

    private void sendEvent(String key, Map<String, Object> event, int attempt) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                eventFailureCounter.increment();
                log.error("Failed to send order event: attempt={}/{}, error={}", attempt, MAX_RETRIES, ex.getMessage());

                if (attempt < MAX_RETRIES) {
                    sendEvent(key, event, attempt + 1);
                } else {
                    sendToDlq(key, event, ex);
                }
            } else {
                eventSuccessCounter.increment();
                log.info("Order event sent successfully: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    private void sendToDlq(String key, Map<String, Object> event, Throwable error) {
        try {
            kafkaTemplate.send(kafkaConfig.getOrderEventsDltTopic(), key, event).get(10, java.util.concurrent.TimeUnit.SECONDS);
            eventDlqCounter.increment();
            log.error("Order event moved to DLQ: key={}, error={}", key, error.getMessage());
        } catch (Exception e) {
            log.error("Failed to send order event to DLQ: key={}, error={}", key, e.getMessage());
        }
    }
}
