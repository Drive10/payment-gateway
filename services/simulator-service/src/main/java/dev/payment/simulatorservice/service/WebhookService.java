package dev.payment.simulatorservice.service;

import dev.payment.simulatorservice.dto.WebhookCallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebClient webClient;
    private final String webhookServiceUrl;
    private final int successRate;
    private final String simulatorMode;
    private final Random random = new Random();

    public WebhookService(
            WebClient.Builder webClientBuilder,
            @Value("${webhook.service-url}") String webhookServiceUrl,
            @Value("${webhook.simulator-success-rate:90}") int successRate,
            @Value("${webhook.simulator-mode:random}") String simulatorMode
    ) {
        this.webhookServiceUrl = webhookServiceUrl;
        this.successRate = successRate;
        this.simulatorMode = simulatorMode;
        this.webClient = webClientBuilder.baseUrl(webhookServiceUrl).build();
    }

    public void sendCallback(UUID transactionId, String paymentReference, String providerOrderId,
                            String providerPaymentId, String status, java.math.BigDecimal amount,
                            String currency) {
        int delaySeconds = 1 + random.nextInt(5);
        log.info("Scheduling webhook callback for transaction {} with {}s delay", transactionId, delaySeconds);

        Mono.delay(Duration.ofSeconds(delaySeconds))
                .flatMap(tick -> {
                    String resolvedStatus = determineStatus();
                    WebhookCallbackRequest callback = new WebhookCallbackRequest(
                            transactionId,
                            paymentReference,
                            providerOrderId,
                            providerPaymentId,
                            resolvedStatus,
                            amount,
                            currency,
                            Instant.now()
                    );

                    log.info("Sending webhook callback for transaction {} with status {}", transactionId, resolvedStatus);

                    return webClient.post()
                            .uri("/webhook/simulator")
                            .bodyValue(callback)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v -> log.info("Webhook callback sent successfully for transaction {}", transactionId))
                            .doOnError(e -> log.error("Failed to send webhook callback for transaction {}: {}", transactionId, e.getMessage()));
                })
                .subscribe();
    }

    private String determineStatus() {
        return switch (simulatorMode.toLowerCase()) {
            case "success" -> "SUCCESS";
            case "failure" -> "FAILED";
            default -> random.nextInt(100) < successRate ? "SUCCESS" : "FAILED";
        };
    }
}
