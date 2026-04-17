package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.enums.PaymentMethod;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static io.micrometer.core.instrument.Meter.Type.GAUGE;
import static io.micrometer.core.instrument.Meter.Type.COUNTER;

@Service
public class PaymentMetricsService {

    private static final String METRIC_PREFIX = "payment";
    
    private final MeterRegistry registry;
    private final AtomicLong activePaymentsGauge = new AtomicLong(0);
    private final AtomicLong pendingWebhooksGauge = new AtomicLong(0);

    private final Counter paymentCreatedCounter;
    private final Counter paymentCapturedCounter;
    private final Counter paymentFailedCounter;
    private final Counter paymentRefundedCounter;
    private final Counter webhookReceivedCounter;
    private final Counter webhookProcessedCounter;
    private final Counter webhookDuplicatedCounter;
    private final Counter idempotencyHitsCounter;
    private final Counter retrySuccessCounter;
    private final Counter retryFailedCounter;
    private final Counter circuitBreakerChangeCounter;

    private final Timer paymentCreationTimer;
    private final Timer paymentCaptureTimer;
    private final Timer webhookProcessingTimer;
    private final Timer kafkaPublishTimer;

    public PaymentMetricsService(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder(METRIC_PREFIX + ".active.count", activePaymentsGauge, AtomicLong::get)
                .description("Number of currently active (non-terminal) payments")
                .register(registry);
                
        Gauge.builder(METRIC_PREFIX + ".webhook.pending.count", pendingWebhooksGauge, AtomicLong::get)
                .description("Number of pending webhook deliveries")
                .register(registry);

        this.paymentCreatedCounter = Counter.builder(METRIC_PREFIX + ".created.total")
                .description("Total payments created")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCapturedCounter = Counter.builder(METRIC_PREFIX + ".captured.total")
                .description("Total payments successfully captured")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentFailedCounter = Counter.builder(METRIC_PREFIX + ".failed.total")
                .description("Total payments that failed")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentRefundedCounter = Counter.builder(METRIC_PREFIX + ".refunded.total")
                .description("Total refunds processed")
                .tag("service", "payment-service")
                .register(registry);

        this.webhookReceivedCounter = Counter.builder("webhook.received.total")
                .description("Total webhooks received")
                .tag("service", "payment-service")
                .register(registry);

        this.webhookProcessedCounter = Counter.builder("webhook.processed.total")
                .description("Total webhooks processed successfully")
                .tag("service", "payment-service")
                .register(registry);

        this.webhookDuplicatedCounter = Counter.builder("webhook.duplicated.total")
                .description("Total duplicate webhooks rejected")
                .tag("service", "payment-service")
                .register(registry);

        this.idempotencyHitsCounter = Counter.builder("idempotency.hits.total")
                .description("Total idempotency cache hits")
                .tag("service", "payment-service")
                .register(registry);

        this.retrySuccessCounter = Counter.builder(METRIC_PREFIX + ".retry.success.total")
                .description("Total successful payment retries")
                .tag("service", "payment-service")
                .register(registry);

        this.retryFailedCounter = Counter.builder(METRIC_PREFIX + ".retry.failed.total")
                .description("Total failed payment retries")
                .tag("service", "payment-service")
                .register(registry);

        this.circuitBreakerChangeCounter = Counter.builder(METRIC_PREFIX + ".circuit_breaker.state")
                .description("Circuit breaker state changes")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCreationTimer = Timer.builder(METRIC_PREFIX + ".creation.duration")
                .description("Payment creation latency")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCaptureTimer = Timer.builder(METRIC_PREFIX + ".capture.duration")
                .description("Payment capture latency")
                .tag("service", "payment-service")
                .register(registry);

        this.webhookProcessingTimer = Timer.builder("webhook.processing.duration")
                .description("Webhook processing latency")
                .tag("service", "payment-service")
                .register(registry);

        this.kafkaPublishTimer = Timer.builder("kafka.publish.duration")
                .description("Kafka event publish latency")
                .tag("service", "payment-service")
                .register(registry);
    }

    public void recordPaymentCreated() {
        paymentCreatedCounter.increment();
        activePaymentsGauge.incrementAndGet();
    }

    public void recordPaymentCaptured() {
        paymentCapturedCounter.increment();
        activePaymentsGauge.decrementAndGet();
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
        activePaymentsGauge.decrementAndGet();
    }

    public void recordPaymentRefunded() {
        paymentRefundedCounter.increment();
    }

    public void recordWebhookReceived() {
        webhookReceivedCounter.increment();
        pendingWebhooksGauge.incrementAndGet();
    }

    public void recordWebhookProcessed() {
        webhookProcessedCounter.increment();
        pendingWebhooksGauge.decrementAndGet();
    }

    public void recordWebhookDuplicated() {
        webhookDuplicatedCounter.increment();
    }

    public void recordIdempotencyHit() {
        idempotencyHitsCounter.increment();
    }

    public void recordRetrySuccess() {
        retrySuccessCounter.increment();
    }

    public void recordRetryFailed() {
        retryFailedCounter.increment();
    }

    public void recordCircuitBreakerStateChange(String circuitName, String newState) {
        Counter.builder(METRIC_PREFIX + ".circuit_breaker.state")
                .tag("circuit", circuitName)
                .tag("state", newState)
                .description("Circuit breaker state changes")
                .register(registry)
                .increment();
    }

    public long timePaymentCreation(Supplier<?> operation) {
        return timeOperation(paymentCreationTimer, operation);
    }

    public long timePaymentCapture(Supplier<?> operation) {
        return timeOperation(paymentCaptureTimer, operation);
    }

    public long timeWebhookProcessing(Supplier<?> operation) {
        return timeOperation(webhookProcessingTimer, operation);
    }

    public long timeKafkaPublish(Supplier<?> operation) {
        return timeOperation(kafkaPublishTimer, operation);
    }

    private long timeOperation(Timer timer, Supplier<?> operation) {
        long start = System.nanoTime();
        try {
            operation.get();
            return System.nanoTime() - start;
        } finally {
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    public void recordProviderLatency(String provider, long latencyMs) {
        Timer.builder(METRIC_PREFIX + ".provider.latency")
                .description("Payment provider response latency")
                .tag("service", "payment-service")
                .tag("provider", provider)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordPaymentMethod(String method, String status) {
        Counter.builder(METRIC_PREFIX + ".method.total")
                .description("Payments by method and status")
                .tag("service", "payment-service")
                .tag("method", method)
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void recordPaymentAmount(PaymentMethod method, String provider, BigDecimal amount, String currency) {
        Counter.builder(METRIC_PREFIX + ".amount.total")
                .tag("method", method != null ? method.name() : "UNKNOWN")
                .tag("provider", provider != null ? provider : "UNKNOWN")
                .tag("currency", currency != null ? currency : "USD")
                .description("Total payment amount processed")
                .register(registry)
                .increment(amount.doubleValue());
    }

    public void recordProcessingTime(String provider, PaymentStatus status, long durationMs) {
        Timer.builder(METRIC_PREFIX + ".processing.time")
                .tag("provider", provider != null ? provider : "UNKNOWN")
                .tag("status", status != null ? status.name() : "UNKNOWN")
                .description("End-to-end payment processing time")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordOutboxEvent(String eventType, String outcome) {
        Counter.builder(METRIC_PREFIX + ".outbox.event")
                .tag("event_type", eventType)
                .tag("outcome", outcome)
                .description("Outbox events")
                .register(registry)
                .increment();
    }

    public void recordWebhookDeliveryLatency(String provider, long latencyMs) {
        Timer.builder("webhook.delivery.latency")
                .tag("provider", provider != null ? provider : "UNKNOWN")
                .description("Webhook delivery latency")
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public double getSuccessRate(String provider) {
        Counter successCounter = registry.find(METRIC_PREFIX + ".captured.total")
                .tag("service", "payment-service")
                .counter();
        Counter totalCounter = registry.find(METRIC_PREFIX + ".created.total")
                .tag("service", "payment-service")
                .counter();
        
        if (successCounter == null || totalCounter == null) {
            return 0.0;
        }
        
        double success = successCounter.count();
        double total = totalCounter.count();
        
        return total > 0 ? success / total : 0.0;
    }

    public void recordWebhookReceivedByProvider(String provider) {
        Counter.builder("webhook.received.by_provider")
                .tag("provider", provider != null ? provider : "UNKNOWN")
                .description("Webhooks received by provider")
                .register(registry)
                .increment();
    }
}