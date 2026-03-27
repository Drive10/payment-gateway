package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PaymentOutboxService {

    private final PaymentOutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentOutboxService(PaymentOutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String aggregateType, String aggregateId, String eventType, String eventKey, String topicName, PaymentEventMessage message) {
        try {
            PaymentOutboxEvent outboxEvent = new PaymentOutboxEvent();
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setEventType(eventType);
            outboxEvent.setEventKey(eventKey);
            outboxEvent.setTopicName(topicName);
            outboxEvent.setPayload(objectMapper.writeValueAsString(message));
            outboxEvent.setCorrelationId(message.correlationId());
            outboxEvent.setStatus(OutboxEventStatus.PENDING);
            outboxEvent.setAttemptCount(0);
            outboxEvent.setNextAttemptAt(Instant.now());
            repository.save(outboxEvent);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_ENQUEUE_FAILED", "Unable to persist payment outbox event");
        }
    }
}
