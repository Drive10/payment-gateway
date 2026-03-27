package dev.payment.ledgerservice.service;

import dev.payment.common.events.PaymentEventMessage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentEventListenerTest {

    @Test
    void shouldRouteCaptureEventsToLedgerPostingService() {
        LedgerEventPostingService ledgerEventPostingService = mock(LedgerEventPostingService.class);
        PaymentEventListener listener = new PaymentEventListener(ledgerEventPostingService);

        listener.onPaymentEvent(message("payment.captured", Map.of()));

        verify(ledgerEventPostingService, times(1)).postCapture(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldIgnoreNonLedgerEvents() {
        LedgerEventPostingService ledgerEventPostingService = mock(LedgerEventPostingService.class);
        PaymentEventListener listener = new PaymentEventListener(ledgerEventPostingService);

        listener.onPaymentEvent(message("payment.created", Map.of("actor", "system")));

        verifyNoInteractions(ledgerEventPostingService);
    }

    private PaymentEventMessage message(String eventType, Map<String, String> metadata) {
        return new PaymentEventMessage(
                UUID.randomUUID(),
                "v1",
                eventType,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "order-ref-1",
                "RAZORPAY_SIMULATOR",
                "CAPTURED",
                "TEST",
                true,
                new BigDecimal("2499.00"),
                BigDecimal.ZERO,
                "INR",
                Instant.now(),
                metadata,
                "corr-123"
        );
    }
}
