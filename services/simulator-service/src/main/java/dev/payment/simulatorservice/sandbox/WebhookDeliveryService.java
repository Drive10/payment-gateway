package dev.payment.simulatorservice.sandbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebClient webClient;

    public WebhookDeliveryService(
            @Value("${simulator.webhook.timeout-ms:5000}") int timeoutMs
    ) {
        this.webClient = WebClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Webhook-Signature", "sandbox-signature")
                .build();
    }

    public void deliver(WebhookScheduler.PendingWebhook webhook) {
        if (webhook.webhookUrl() == null || webhook.webhookUrl().isBlank()) {
            log.debug("No webhook URL for transaction: {}", webhook.transactionId());
            return;
        }

        log.info("Delivering webhook event={} for transaction={} to {}",
                webhook.eventType(), webhook.transactionId(), webhook.webhookUrl());

        webClient.post()
                .uri(webhook.webhookUrl())
                .bodyValue(webhook.payload())
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(5000))
                .block();

        log.info("Webhook delivered successfully: {}", webhook.transactionId());
    }

    public void deliverSync(WebhookScheduler.PendingWebhook webhook) {
        deliver(webhook);
    }
}