package dev.payment.paymentservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMetricsServiceTest {

    private MeterRegistry registry;
    private PaymentMetricsService metricsService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricsService = new PaymentMetricsService(registry);
    }

    @Test
    void recordPaymentCreated_shouldIncrementCounter() {
        metricsService.recordPaymentCreated();
        metricsService.recordPaymentCreated();

        double count = registry.get("payment.created.total").counter().count();
        assertEquals(2.0, count);
    }

    @Test
    void recordPaymentCaptured_shouldIncrementCounter() {
        metricsService.recordPaymentCaptured();

        double count = registry.get("payment.captured.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordPaymentFailed_shouldIncrementCounter() {
        metricsService.recordPaymentFailed();

        double count = registry.get("payment.failed.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordPaymentRefunded_shouldIncrementCounter() {
        metricsService.recordPaymentRefunded();

        double count = registry.get("payment.refunded.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordWebhookReceived_shouldIncrementCounter() {
        metricsService.recordWebhookReceived();
        metricsService.recordWebhookReceived();
        metricsService.recordWebhookReceived();

        double count = registry.get("webhook.received.total").counter().count();
        assertEquals(3.0, count);
    }

    @Test
    void recordWebhookDuplicated_shouldIncrementCounter() {
        metricsService.recordWebhookDuplicated();

        double count = registry.get("webhook.duplicated.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordIdempotencyHit_shouldIncrementCounter() {
        metricsService.recordIdempotencyHit();
        metricsService.recordIdempotencyHit();

        double count = registry.get("idempotency.hits.total").counter().count();
        assertEquals(2.0, count);
    }

    @Test
    void recordRetrySuccess_shouldIncrementCounter() {
        metricsService.recordRetrySuccess();

        double count = registry.get("payment.retry.success.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordRetryFailed_shouldIncrementCounter() {
        metricsService.recordRetryFailed();

        double count = registry.get("payment.retry.failed.total").counter().count();
        assertEquals(1.0, count);
    }

    @Test
    void recordProviderLatency_shouldRecordTimer() {
        metricsService.recordProviderLatency("stripe", 150);
        metricsService.recordProviderLatency("razorpay", 200);

        assertNotNull(registry.get("payment.provider.latency").tag("provider", "stripe").timer());
        assertNotNull(registry.get("payment.provider.latency").tag("provider", "razorpay").timer());
    }

    @Test
    void recordPaymentMethod_shouldIncrementMethodCounter() {
        metricsService.recordPaymentMethod("CARD", "CAPTURED");
        metricsService.recordPaymentMethod("UPI", "CAPTURED");
        metricsService.recordPaymentMethod("CARD", "FAILED");

        assertEquals(1.0, registry.get("payment.method.total").tag("method", "CARD").tag("status", "CAPTURED").counter().count());
        assertEquals(1.0, registry.get("payment.method.total").tag("method", "UPI").tag("status", "CAPTURED").counter().count());
        assertEquals(1.0, registry.get("payment.method.total").tag("method", "CARD").tag("status", "FAILED").counter().count());
    }

    @Test
    void timePaymentCreation_shouldRecordTimer() {
        metricsService.timePaymentCreation(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        assertNotNull(registry.get("payment.creation.duration").timer());
        assertTrue(registry.get("payment.creation.duration").timer().count() > 0);
    }
}