package dev.payment.paymentservice.kafka;

import dev.payment.paymentservice.service.PaymentMetricsService;
import dev.payment.paymentservice.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class DeadLetterQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueueConsumer.class);

    private final AuditService auditService;
    private final PaymentMetricsService metricsService;
    private final ObjectMapper objectMapper;

    public DeadLetterQueueConsumer(
            AuditService auditService,
            PaymentMetricsService metricsService,
            ObjectMapper objectMapper
    ) {
        this.auditService = auditService;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${application.kafka.topic.payment-events-dlt:payment.events.dlt}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}-dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetter(ConsumerRecord<String, Object> record) {
        String correlationId = extractCorrelationId(record);
        MDC.put("correlationId", correlationId != null ? correlationId : record.key());
        
        try {
            log.error("event=dlq_message_received topic={} partition={} offset={} key={} correlationId={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    correlationId);

            Map<String, Object> payload = parsePayload(record);
            String eventType = extractEventType(payload);
            
            auditService.record(
                    "DLQ_MESSAGE_RECEIVED",
                    "SYSTEM",
                    "KAFKA_DLQ",
                    record.key(),
                    String.format("DLQ message from topic %s: eventType=%s", record.topic(), eventType)
            );

            metricsService.recordOutboxEvent(eventType != null ? eventType : "UNKNOWN", "DLQ");

            handleMessageByType(record, payload, eventType);

        } catch (Exception e) {
            log.error("event=dlq_processing_failed key={} error={}", record.key(), e.getMessage(), e);
            metricsService.recordOutboxEvent("UNKNOWN", "DLQ_FAILED");
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(ConsumerRecord<String, Object> record) {
        try {
            if (record.headers() != null) {
                var header = record.headers().lastHeader("correlation-id");
                if (header != null) {
                    return new String(header.value());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract correlation ID from record headers");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(ConsumerRecord<String, Object> record) {
        try {
            if (record.value() instanceof Map) {
                return (Map<String, Object>) record.value();
            }
            return objectMapper.readValue(objectMapper.writeValueAsString(record.value()), Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse payload, using empty map");
            return Map.of();
        }
    }

    private String extractEventType(Map<String, Object> payload) {
        if (payload.containsKey("eventType")) {
            return String.valueOf(payload.get("eventType"));
        }
        return null;
    }

    private void handleMessageByType(ConsumerRecord<String, Object> record, Map<String, Object> payload, String eventType) {
        if (eventType == null) {
            log.warn("event=dlq_unknown_event_type key={}", record.key());
            return;
        }

        switch (eventType) {
            case "payment.created" -> handlePaymentCreatedDq(record, payload);
            case "payment.captured" -> handlePaymentCapturedDq(record, payload);
            case "payment.failed" -> handlePaymentFailedDq(record, payload);
            case "payment.refunded" -> handlePaymentRefundedDq(record, payload);
            default -> log.warn("event=dlq_unhandled_event_type eventType={}", eventType);
        }
    }

    private void handlePaymentCreatedDq(ConsumerRecord<String, Object> record, Map<String, Object> payload) {
        log.error("event=dlq_payment_created_requires_review key={} paymentId={}",
                record.key(), payload.get("paymentId"));
        
        auditService.record(
                "DLQ_PAYMENT_CREATED_REQUIRES_REVIEW",
                "SYSTEM",
                "PAYMENT",
                String.valueOf(payload.get("paymentId")),
                "Payment created event in DLQ - requires manual investigation"
        );
    }

    private void handlePaymentCapturedDq(ConsumerRecord<String, Object> record, Map<String, Object> payload) {
        log.error("event=dlq_payment_captured_requires_review key={} paymentId={}",
                record.key(), payload.get("paymentId"));
        
        auditService.record(
                "DLQ_PAYMENT_CAPTURED_REQUIRES_REVIEW",
                "SYSTEM",
                "PAYMENT",
                String.valueOf(payload.get("paymentId")),
                "Payment captured event in DLQ - requires manual investigation. Order may not have been updated."
        );
    }

    private void handlePaymentFailedDq(ConsumerRecord<String, Object> record, Map<String, Object> payload) {
        log.warn("event=dlq_payment_failed key={} paymentId={}",
                record.key(), payload.get("paymentId"));
        
        auditService.record(
                "DLQ_PAYMENT_FAILED_LOGGED",
                "SYSTEM",
                "PAYMENT",
                String.valueOf(payload.get("paymentId")),
                "Payment failed event logged from DLQ"
        );
    }

    private void handlePaymentRefundedDq(ConsumerRecord<String, Object> record, Map<String, Object> payload) {
        log.error("event=dlq_payment_refunded_requires_review key={} paymentId={}",
                record.key(), payload.get("paymentId"));
        
        auditService.record(
                "DLQ_PAYMENT_REFUNDED_REQUIRES_REVIEW",
                "SYSTEM",
                "PAYMENT",
                String.valueOf(payload.get("paymentId")),
                "Payment refunded event in DLQ - requires manual investigation. Ledger may need reconciliation."
        );
    }
}
