package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

    @Mock
    private PaymentOutboxEventRepository repository;

    @Mock
    private KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;

    private PaymentOutboxRelay relay;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        relay = new PaymentOutboxRelay(repository, kafkaTemplate, objectMapper);
    }

    @Test
    void shouldPublishPendingOutboxEventsWithTraceHeaders() throws Exception {
        PaymentOutboxEvent event = eventWithAttempts(0);
        event.setMessageHeaders(objectMapper.writeValueAsString(Map.of("traceparent", "00-abcd-ef01-01")));

        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        relay.relayPendingEvents();

        ArgumentCaptor<ProducerRecord<String, PaymentEventMessage>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, PaymentEventMessage> record = captor.getValue();

        assertThat(record.topic()).isEqualTo("payment.events");
        assertThat(new String(record.headers().lastHeader("traceparent").value(), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("00-abcd-ef01-01");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void shouldScheduleRetryWhenKafkaPublishFails() {
        PaymentOutboxEvent event = eventWithAttempts(0);
        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn((CompletableFuture) failed);

        relay.relayPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(event.getLastError()).contains("kafka unavailable");
    }

    @Test
    void shouldMoveEventToDeadLetterAfterMaxAttempts() {
        PaymentOutboxEvent event = eventWithAttempts(2);
        when(repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of(event));

        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("still failing"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn((CompletableFuture) failed);

        relay.relayPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DEAD_LETTER);
        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getLastError()).contains("still failing");
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
}
