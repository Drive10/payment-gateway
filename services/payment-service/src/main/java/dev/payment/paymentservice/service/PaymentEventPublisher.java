package dev.payment.paymentservice.service;

import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentEventPublisher {

    private static final String EVENT_SCHEMA_VERSION = "v1";

    private final PaymentOutboxService paymentOutboxService;
    private final KafkaTraceHeaderFactory kafkaTraceHeaderFactory;
    private final String topicName;

    public PaymentEventPublisher(
            PaymentOutboxService paymentOutboxService,
            KafkaTraceHeaderFactory kafkaTraceHeaderFactory,
            @Value("${application.kafka.topic.payment-events}") String topicName
    ) {
        this.paymentOutboxService = paymentOutboxService;
        this.kafkaTraceHeaderFactory = kafkaTraceHeaderFactory;
        this.topicName = topicName;
    }

    public void publish(String eventType, Payment payment, Map<String, String> metadata) {
        Map<String, String> eventMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            eventMetadata.putAll(metadata);
        }

        UUID orderId = payment.getOrderId();
        String orderRef = orderId != null ? "ORD-" + orderId.toString().substring(0, 8).toUpperCase() : "ORD-UNKNOWN";

        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID(),
                EVENT_SCHEMA_VERSION,
                eventType,
                payment.getId(),
                orderId,
                orderRef,
                payment.getProvider(),
                payment.getStatus().name(),
                payment.getTransactionMode().name(),
                payment.isSimulated(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getCurrency(),
                Instant.now(),
                eventMetadata,
                org.slf4j.MDC.get("correlationId")
        );

        paymentOutboxService.enqueue(
                "PAYMENT",
                payment.getId().toString(),
                eventType,
                payment.getId().toString(),
                topicName,
                message,
                kafkaTraceHeaderFactory.currentHeaders()
        );
    }
}
