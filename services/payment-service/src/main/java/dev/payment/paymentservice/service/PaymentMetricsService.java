package dev.payment.paymentservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class PaymentMetricsService {

    private final MeterRegistry registry;

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

    private final Timer paymentCreationTimer;
    private final Timer paymentCaptureTimer;
    private final Timer webhookProcessingTimer;
    private final Timer kafkaPublishTimer;

    public PaymentMetricsService(MeterRegistry registry) {
        this.registry = registry;

        this.paymentCreatedCounter = Counter.builder("payment.created.total")
                .description("Total payments created")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCapturedCounter = Counter.builder("payment.captured.total")
                .description("Total payments successfully captured")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentFailedCounter = Counter.builder("payment.failed.total")
                .description("Total payments that failed")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentRefundedCounter = Counter.builder("payment.refunded.total")
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

        this.retrySuccessCounter = Counter.builder("payment.retry.success.total")
                .description("Total successful payment retries")
                .tag("service", "payment-service")
                .register(registry);

        this.retryFailedCounter = Counter.builder("payment.retry.failed.total")
                .description("Total failed payment retries")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCreationTimer = Timer.builder("payment.creation.duration")
                .description("Payment creation latency")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentCaptureTimer = Timer.builder("payment.capture.duration")
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
    }

    public void recordPaymentCaptured() {
        paymentCapturedCounter.increment();
    }

    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
    }

    public void recordPaymentRefunded() {
        paymentRefundedCounter.increment();
    }

    public void recordWebhookReceived() {
        webhookReceivedCounter.increment();
    }

    public void recordWebhookProcessed() {
        webhookProcessedCounter.increment();
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
        Timer.builder("payment.provider.latency")
                .description("Payment provider response latency")
                .tag("service", "payment-service")
                .tag("provider", provider)
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordPaymentMethod(String method, String status) {
        Counter.builder("payment.method.total")
                .description("Payments by method and status")
                .tag("service", "payment-service")
                .tag("method", method)
                .tag("status", status)
                .register(registry)
                .increment();
    }
}