package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class PaymentOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxRelay.class);
    private static final int MAX_ATTEMPTS = 3;

    private final PaymentOutboxEventRepository repository;
    private final KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentOutboxRelay(
            PaymentOutboxEventRepository repository,
            KafkaTemplate<String, PaymentEventMessage> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${application.outbox.relay-delay-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<PaymentOutboxEvent> events = repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                Instant.now()
        );

        for (PaymentOutboxEvent event : events) {
            relay(event);
        }
    }

    private void relay(PaymentOutboxEvent event) {
        try {
            PaymentEventMessage message = objectMapper.readValue(event.getPayload(), PaymentEventMessage.class);
            ProducerRecord<String, PaymentEventMessage> record = new ProducerRecord<>(event.getTopicName(), event.getEventKey(), message);
            Map<String, String> headers = event.getMessageHeaders() == null || event.getMessageHeaders().isBlank()
                    ? java.util.Collections.emptyMap()
                    : objectMapper.readValue(event.getMessageHeaders(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            headers.forEach((key, value) -> record.headers().add(key, value.getBytes(StandardCharsets.UTF_8)));
            kafkaTemplate.send(record).get();
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
        } catch (Exception exception) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(truncate(exception.getMessage()));
            event.setNextAttemptAt(Instant.now().plusSeconds(5L * attempts));
            if (attempts >= MAX_ATTEMPTS) {
                event.setStatus(OutboxEventStatus.DEAD_LETTER);
                log.error("event=payment_outbox_dead_letter outboxId={} eventType={} alert=true reason={}",
                        event.getId(), event.getEventType(), truncate(exception.getMessage()));
            } else {
                log.warn("event=payment_outbox_retry outboxId={} eventType={} attempt={} reason={}",
                        event.getId(), event.getEventType(), attempts, truncate(exception.getMessage()));
            }
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
