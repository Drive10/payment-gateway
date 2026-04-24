package dev.payment.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.notificationservice.dto.ForwardWebhookRequest;
import dev.payment.notificationservice.dto.WebhookPayload;
import dev.payment.notificationservice.domain.entities.WebhookEvent;
import dev.payment.notificationservice.domain.entities.WebhookStatus;
import dev.payment.notificationservice.exception.WebhookValidationException;
import dev.payment.notificationservice.infrastructure.persistence.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookValidator webhookValidator;
    private final ObjectMapper objectMapper;
    private final String paymentServiceUrl;
    private final RestTemplate restTemplate;
    private final MetricsService metricsService;

    public WebhookService(
            WebhookEventRepository webhookEventRepository,
            WebhookValidator webhookValidator,
            ObjectMapper objectMapper,
            @Value("${application.payment-service.url}") String paymentServiceUrl,
            MetricsService metricsService
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookValidator = webhookValidator;
        this.objectMapper = objectMapper;
        this.paymentServiceUrl = paymentServiceUrl;
        this.metricsService = metricsService;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public WebhookEvent processWebhook(String provider, String payload, String signature, String timestamp, String eventId) {
        if (signature != null) {
            webhookValidator.validateSignature(signature, payload);
        }
        if (timestamp != null) {
            webhookValidator.validateTimestamp(timestamp);
        }

        Optional<WebhookEvent> existingEvent = findExistingEvent(provider, eventId);
        if (existingEvent.isPresent()) {
            WebhookEvent existing = existingEvent.get();
            if (existing.getStatus() == WebhookStatus.PROCESSED) {
                log.info("Duplicate webhook event detected - eventId: {}, provider: {}, returning existing", 
                        eventId, provider);
                metricsService.incrementWebhookDuplicate();
                return existing;
            }
            log.info("Resuming processing for webhook event {} with status {}", existing.getId(), existing.getStatus());
            return existing;
        }

        WebhookEvent event = new WebhookEvent();
        event.setEventId(eventId);
        event.setEventType(extractEventType(payload));
        event.setProvider(provider);
        event.setPayload(payload);
        event.setSignature(signature);
        event.setTimestampHeader(timestamp);
        event.setStatus(WebhookStatus.RECEIVED);
        event.setAttempt(0);
        event = webhookEventRepository.save(event);

        log.info("Processing new webhook event: id={}, provider={}, eventType={}, eventId={}", 
                event.getId(), provider, event.getEventType(), eventId);
        metricsService.incrementWebhookReceived(provider);

        boolean success = deliverWebhookWithRetry(event);

        if (success) {
            event.setStatus(WebhookStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
            log.info("Successfully processed webhook event {} for provider {}", event.getId(), provider);
            metricsService.incrementWebhookDelivered(provider);
        } else {
            event.setStatus(WebhookStatus.PENDING_RETRY);
            event.setNextRetryAt(Instant.now().plus(INITIAL_BACKOFF));
            webhookEventRepository.save(event);
            log.warn("Webhook event {} failed after {} attempts, marked for retry", event.getId(), MAX_RETRY_ATTEMPTS);
            metricsService.incrementWebhookFailed(provider);
        }

        return event;
    }

    private Optional<WebhookEvent> findExistingEvent(String provider, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Optional.empty();
        }
        return webhookEventRepository.findByProviderAndEventId(provider, eventId);
    }

    private boolean deliverWebhookWithRetry(WebhookEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                event.setStatus(WebhookStatus.PROCESSING);
                event.setAttempt(attempt);
                webhookEventRepository.save(event);

                forwardToPaymentService(event);

                return true;
            } catch (WebhookDeliveryException e) {
                log.warn("Webhook delivery attempt {} failed for event {}: {}", attempt, event.getId(), e.getMessage());
                event.setErrorMessage(e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Duration backoff = calculateBackoff(attempt);
                    event.setNextRetryAt(Instant.now().plus(backoff));
                    webhookEventRepository.save(event);
                    
                    try {
                        Thread.sleep(backoff.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return false;
    }

    private Duration calculateBackoff(int attempt) {
        Duration backoff = INITIAL_BACKOFF.multipliedBy((long) Math.pow(2, attempt - 1));
        return backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff;
    }

    @Scheduled(fixedDelayString = "${application.webhook.retry.interval-ms:30000}")
    @Transactional
    public void processRetries() {
        Instant now = Instant.now();
        webhookEventRepository.findPendingRetries(now, MAX_RETRY_ATTEMPTS)
                .forEach(event -> {
                    log.info("Retrying webhook event: id={}, attempt={}", event.getId(), event.getAttempt() + 1);
                    try {
                        forwardToPaymentService(event);
                        event.setStatus(WebhookStatus.PROCESSED);
                        event.setProcessedAt(Instant.now());
                        webhookEventRepository.save(event);
                        metricsService.incrementWebhookDelivered(event.getProvider());
                    } catch (WebhookDeliveryException e) {
                        event.setAttempt(event.getAttempt() + 1);
                        if (event.getAttempt() >= MAX_RETRY_ATTEMPTS) {
                            event.setStatus(WebhookStatus.FAILED);
                            event.setErrorMessage("Max retries exceeded: " + e.getMessage());
                            metricsService.incrementWebhookFailed(event.getProvider());
                        } else {
                            event.setNextRetryAt(Instant.now().plus(calculateBackoff(event.getAttempt())));
                        }
                        event.setErrorMessage(e.getMessage());
                        webhookEventRepository.save(event);
                    }
                });
    }

    private String extractEventType(String payload) {
        try {
            WebhookPayload webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);
            return webhookPayload.eventType();
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse event type from payload, using default");
            return "unknown";
        }
    }

    @Transactional
    public void forwardToPaymentService(WebhookEvent event) {
        try {
            ForwardWebhookRequest request = new ForwardWebhookRequest(
                    event.getId(),
                    event.getEventType(),
                    event.getPayload(),
                    Instant.now()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Event-ID", event.getEventId());
            headers.set("X-Webhook-Provider", event.getProvider());
            HttpEntity<ForwardWebhookRequest> httpRequest = new HttpEntity<>(request, headers);

            String url = paymentServiceUrl + "/api/v1/webhooks/razorpay";
            restTemplate.postForEntity(url, httpRequest, Void.class);

            log.info("Forwarded webhook event {} to payment service", event.getId());
        } catch (RestClientException e) {
            log.error("Failed to forward webhook event {} to payment service: {}", event.getId(), e.getMessage());
            throw new WebhookDeliveryException("Failed to forward webhook to payment service", e);
        }
    }
}
