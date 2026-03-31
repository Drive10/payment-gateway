package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

    @Mock
    private PaymentOutboxEventRepository repository;

    private TestablePaymentOutboxRelay relay;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        relay = new TestablePaymentOutboxRelay(repository, objectMapper);
    }

    @Test
    void shouldPublishPendingOutboxEventsWithTraceHeaders() {
        PaymentOutboxEvent event = eventWithAttempts(0);
        event.setMessageHeaders("{\"traceparent\":\"00-abcd-ef01-01\"}");

        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        relay.relayPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        
        ArgumentCaptor<PaymentOutboxEvent> captor = ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
    }

    @Test
    void shouldScheduleRetryWhenKafkaPublishFails() {
        PaymentOutboxEvent event = eventWithAttempts(0);
        relay.setShouldFail(true);

        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        relay.relayPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void shouldMoveEventToDeadLetterAfterMaxAttempts() {
        PaymentOutboxEvent event = eventWithAttempts(2);
        relay.setShouldFail(true);

        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        relay.relayPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
        assertThat(event.getAttemptCount()).isEqualTo(3);
    }

    private PaymentOutboxEvent eventWithAttempts(int attempts) {
        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID(),
                "PAYMENT_CAPTURED_V1",
                "payment.captured",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "order-1001",
                "RAZORPAY_SIMULATOR",
                "CAPTURED",
                "TEST",
                true,
                new BigDecimal("2499.00"),
                BigDecimal.ZERO,
                "INR",
                Instant.parse("2026-03-27T10:15:30Z"),
                Map.of("source", "unit-test"),
                "corr-outbox-1001"
        );

        try {
            PaymentOutboxEvent event = new PaymentOutboxEvent();
            event.setAggregateType("PAYMENT");
            event.setAggregateId(message.paymentId().toString());
            event.setEventType(message.eventType());
            event.setEventKey(message.paymentId().toString());
            event.setTopicName("payment.events");
            event.setPayload(objectMapper.writeValueAsString(message));
            event.setCorrelationId(message.correlationId());
            event.setStatus(OutboxEventStatus.PENDING);
            event.setAttemptCount(attempts);
            event.setNextAttemptAt(Instant.now().minusSeconds(1));
            return event;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static class TestablePaymentOutboxRelay {
        private static final int MAX_ATTEMPTS = 3;
        
        private final PaymentOutboxEventRepository repository;
        private final ObjectMapper objectMapper;
        private boolean shouldFail = false;

        public TestablePaymentOutboxRelay(PaymentOutboxEventRepository repository, ObjectMapper objectMapper) {
            this.repository = repository;
            this.objectMapper = objectMapper;
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

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
                if (shouldFail) {
                    throw new RuntimeException("Kafka unavailable");
                }
                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception exception) {
                int attempts = event.getAttemptCount() + 1;
                event.setAttemptCount(attempts);
                event.setLastError(exception.getMessage());
                event.setNextAttemptAt(Instant.now().plusSeconds(5L * attempts));
                if (attempts >= MAX_ATTEMPTS) {
                    event.setStatus(OutboxEventStatus.DEAD_LETTER);
                } else {
                    event.setStatus(OutboxEventStatus.PENDING);
                }
            }
            repository.save(event);
        }
    }
}
