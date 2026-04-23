package dev.payment.simulatorservice.service;

import dev.payment.simulatorservice.dto.WebhookCallbackRequest;
import dev.payment.simulatorservice.model.SimulationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebhookDeliveryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, WebhookDelivery> deliveries = new ConcurrentHashMap<>();

    @Value("${webhook.delivery.enabled:true}")
    private boolean enabled;

    @Value("${webhook.delivery.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${webhook.delivery.retry.interval-ms:5000}")
    private long retryIntervalMs;

    @Async
    public void deliver(String providerOrderId, String status, String webhookUrl) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook disabled or no URL, skipping: {}", providerOrderId);
            return;
        }

        String deliveryId = UUID.randomUUID().toString();
        WebhookDelivery delivery = new WebhookDelivery(deliveryId, providerOrderId, status, webhookUrl);
        deliveries.put(deliveryId, delivery);

        deliverWithRetry(delivery);
    }

    private void deliverWithRetry(WebhookDelivery delivery) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                boolean success = sendWebhook(delivery, attempt);
                delivery.recordAttempt(attempt, success);

                if (success) {
                    log.info("Webhook delivered: {} attempt={} deliveryId={}", 
                        delivery.providerOrderId(), attempt, delivery.deliveryId());
                    return;
                }

                if (attempt < maxAttempts) {
                    log.warn("Webhook failed: {} attempt={}/{}, retrying in {}ms", 
                        delivery.providerOrderId(), attempt, maxAttempts, retryIntervalMs);
                    Thread.sleep(retryIntervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Webhook error: {} attempt={} error={}", 
                    delivery.providerOrderId(), attempt, e.getMessage());
            }
        }

        log.error("Webhook FAILED after {} attempts: {}", maxAttempts, delivery.providerOrderId());
        delivery.markFailed();
    }

    private boolean sendWebhook(WebhookDelivery delivery, int attempt) throws Exception {
        WebhookCallbackRequest payload = WebhookCallbackRequest.builder()
                .event("payment." + delivery.status().toLowerCase())
                .providerOrderId(delivery.providerOrderId())
                .status(delivery.status())
                .attempt(attempt)
                .timestamp(Instant.now().toString())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Delivery", delivery.deliveryId());
        headers.set("X-Webhook-Signature", "sha256=" + generateSignature(payload));

        HttpEntity<WebhookCallbackRequest> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(delivery.webhookUrl(), request, String.class);
        return true;
    }

    private String generateSignature(WebhookCallbackRequest payload) {
        return "mock_sig_" + payload.providerOrderId() + "_" + System.currentTimeMillis();
    }

    public Map<String, Object> getDeliveryStatus(String deliveryId) {
        WebhookDelivery delivery = deliveries.get(deliveryId);
        if (delivery == null) {
            return Map.of("status", "NOT_FOUND");
        }
        return Map.of(
            "deliveryId", delivery.deliveryId(),
            "providerOrderId", delivery.providerOrderId(),
            "status", delivery.status(),
            "attempts", delivery.attempts(),
            "success", delivery.success(),
            "lastAttempt", delivery.lastAttempt()
        );
    }

    public Map<String, String> getFailedDeliveries() {
        return deliveries.values().stream()
                .filter(d -> !d.success() && d.attempts() >= maxAttempts)
                .collect(java.util.stream.Collectors.toMap(
                    WebhookDelivery::deliveryId,
                    WebhookDelivery::providerOrderId
                ));
    }

    private static class WebhookDelivery {
        private final String deliveryId;
        private final String providerOrderId;
        private final String status;
        private final String webhookUrl;
        private int attempts = 0;
        private boolean success = false;
        private long lastAttempt = 0;

        WebhookDelivery(String deliveryId, String providerOrderId, String status, String webhookUrl) {
            this.deliveryId = deliveryId;
            this.providerOrderId = providerOrderId;
            this.status = status;
            this.webhookUrl = webhookUrl;
        }

        public String deliveryId() { return deliveryId; }
        public String providerOrderId() { return providerOrderId; }
        public String status() { return status; }
        public String webhookUrl() { return webhookUrl; }
        public int attempts() { return attempts; }
        public boolean success() { return success; }
        public long lastAttempt() { return lastAttempt; }

        void recordAttempt(int attempt, boolean success) {
            this.attempts = attempt;
            this.success = success;
            this.lastAttempt = System.currentTimeMillis();
        }

        void markFailed() {
            this.success = false;
        }
    }
}