package dev.payment.paymentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Timer paymentTimer(MeterRegistry registry) {
        return Timer.builder("payment.request.latency")
                .description("Payment request latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public Counter paymentSuccessCounter(MeterRegistry registry) {
        return Counter.builder("payment.requests")
                .tag("status", "success")
                .description("Successful payment requests")
                .register(registry);
    }

    @Bean
    public Counter paymentFailureCounter(MeterRegistry registry) {
        return Counter.builder("payment.requests")
                .tag("status", "failure")
                .description("Failed payment requests")
                .register(registry);
    }

    @Bean
    public Counter webhookReceivedCounter(MeterRegistry registry) {
        return Counter.builder("webhooks.received")
                .description("Webhooks received")
                .register(registry);
    }

    @Bean
    public Counter webhookProcessedCounter(MeterRegistry registry) {
        return Counter.builder("webhooks.processed")
                .description("Webhooks processed successfully")
                .register(registry);
    }

    @Bean
    public Counter webhookFailedCounter(MeterRegistry registry) {
        return Counter.builder("webhooks.failed")
                .description("Webhooks failed")
                .register(registry);
    }

    @Bean
    public Counter dlqEventsCounter(MeterRegistry registry) {
        return Counter.builder("events.dead_lettered")
                .description("Events moved to dead letter queue")
                .register(registry);
    }

    @Bean
    public Counter outboxPublishedCounter(MeterRegistry registry) {
        return Counter.builder("events.published")
                .description("Outbox events published")
                .register(registry);
    }

    @Bean
    public Counter reconciliationMismatchCounter(MeterRegistry registry) {
        return Counter.builder("reconciliation.mismatches")
                .description("Reconciliation mismatches detected")
                .register(registry);
    }

    @Bean
    public Counter settlementSuccessCounter(MeterRegistry registry) {
        return Counter.builder("settlements.completed")
                .description("Successful settlements")
                .register(registry);
    }

    @Bean
    public Counter settlementFailureCounter(MeterRegistry registry) {
        return Counter.builder("settlements.failed")
                .description("Failed settlements")
                .register(registry);
    }
}