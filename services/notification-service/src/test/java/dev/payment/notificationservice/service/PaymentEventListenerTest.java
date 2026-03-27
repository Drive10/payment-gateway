package dev.payment.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.notificationservice.domain.DeliveryStatus;
import dev.payment.notificationservice.domain.Notification;
import dev.payment.notificationservice.repository.ConsumedEventRepository;
import dev.payment.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ConsumedEventRepository consumedEventRepository;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        listener = new PaymentEventListener(notificationRepository, consumedEventRepository, objectMapper);
    }

    @Test
    void shouldIgnoreDuplicateEventIds() {
        PaymentEventMessage message = sampleEvent();
        when(consumedEventRepository.existsByEventId(message.eventId().toString())).thenReturn(true);

        listener.onPaymentEvent(message);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldPersistFailedNotificationFromDeadLetterTopic() {
        PaymentEventMessage message = sampleEvent();
        when(consumedEventRepository.existsByEventId(message.eventId().toString())).thenReturn(false);

        listener.onDeadLetterEvent(message, "payment.events.dlt");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(captor.getValue().getTemplateCode()).isEqualTo("PAYMENT_CAPTURED");
    }

    private PaymentEventMessage sampleEvent() {
        return new PaymentEventMessage(
                UUID.randomUUID(),
                "v1",
                "payment.captured",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "order-1001",
                "RAZORPAY_PRIMARY",
                "CAPTURED",
                "PRODUCTION",
                false,
                new BigDecimal("2499.00"),
                BigDecimal.ZERO,
                "INR",
                Instant.parse("2026-03-27T10:15:30Z"),
                Map.of("source", "unit-test"),
                "corr-unit-1001"
        );
    }
}
