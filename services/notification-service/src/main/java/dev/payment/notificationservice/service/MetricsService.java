package dev.payment.notificationservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementWebhookReceived(String provider) {
        Counter.builder("webhook.received")
                .tag("provider", provider)
                .description("Total webhooks received")
                .register(meterRegistry)
                .increment();
    }

    public void incrementWebhookDelivered(String provider) {
        Counter.builder("webhook.delivered")
                .tag("provider", provider)
                .description("Total webhooks successfully delivered")
                .register(meterRegistry)
                .increment();
    }

    public void incrementWebhookFailed(String provider) {
        Counter.builder("webhook.failed")
                .tag("provider", provider)
                .description("Total webhooks that failed after retries")
                .register(meterRegistry)
                .increment();
    }

    public void incrementWebhookDuplicate() {
        Counter.builder("webhook.duplicate")
                .description("Total duplicate webhooks detected")
                .register(meterRegistry)
                .increment();
    }
}
