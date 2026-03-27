package dev.payment.paymentservice.service;

import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;
    private final String topicName;

    public PaymentEventPublisher(
            KafkaTemplate<String, PaymentEventMessage> kafkaTemplate,
            @Value("${application.kafka.topic.payment-events}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(String eventType, Payment payment, Map<String, String> metadata) {
        Map<String, String> eventMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            eventMetadata.putAll(metadata);
        }

        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID(),
                eventType,
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderReference(),
                payment.getProvider(),
                payment.getStatus().name(),
                payment.getTransactionMode().name(),
                payment.isSimulated(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getCurrency(),
                Instant.now(),
                eventMetadata
        );

        kafkaTemplate.send(topicName, payment.getId().toString(), message);
    }
}
