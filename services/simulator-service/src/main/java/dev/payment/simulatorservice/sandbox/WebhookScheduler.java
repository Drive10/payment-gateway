package dev.payment.simulatorservice.sandbox;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WebhookScheduler {

    private final WebhookDeliveryService webhookDeliveryService;
    private final Queue<PendingWebhook> pendingWebhooks = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    @PostConstruct
    public void init() {
        log.info("WebhookScheduler initialized - async webhooks enabled");
    }

    public void scheduleWebhook(PendingWebhook webhook) {
        if (!enabled.get()) {
            log.debug("WebhookScheduler disabled, skipping: {}", webhook.transactionId());
            return;
        }
        pendingWebhooks.offer(webhook);
        log.debug("Scheduled webhook for transaction: {}", webhook.transactionId());
    }

    @Scheduled(fixedDelayString = "${simulator.webhook.scheduler.delay-ms:500}")
    public void processPendingWebhooks() {
        PendingWebhook webhook;
        while ((webhook = pendingWebhooks.poll()) != null) {
            try {
                webhookDeliveryService.deliver(webhook);
            } catch (Exception e) {
                log.error("Failed to deliver webhook: {}", webhook.transactionId(), e);
                if (webhook.retryCount() < 3) {
                    pendingWebhooks.offer(webhook.incrementRetry());
                }
            }
        }
    }

    public void disable() {
        enabled.set(false);
    }

    public void enable() {
        enabled.set(true);
    }

    public record PendingWebhook(
            String transactionId,
            String providerOrderId,
            String webhookUrl,
            String eventType,
            Object payload,
            int retryCount
    ) {
        public PendingWebhook incrementRetry() {
            return new PendingWebhook(
                    transactionId,
                    providerOrderId,
                    webhookUrl,
                    eventType,
                    payload,
                    retryCount + 1
            );
        }
    }
}