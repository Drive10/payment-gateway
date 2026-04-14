package dev.payment.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.notificationservice.dto.ForwardWebhookRequest;
import dev.payment.notificationservice.dto.WebhookPayload;
import dev.payment.notificationservice.entity.WebhookEvent;
import dev.payment.notificationservice.entity.WebhookStatus;
import dev.payment.notificationservice.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookValidator webhookValidator;
    private final ObjectMapper objectMapper;
    private final String paymentServiceUrl;
    private final RestTemplate restTemplate;

    public WebhookService(
            WebhookEventRepository webhookEventRepository,
            WebhookValidator webhookValidator,
            ObjectMapper objectMapper,
            @Value("${application.payment-service.url}") String paymentServiceUrl
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookValidator = webhookValidator;
        this.objectMapper = objectMapper;
        this.paymentServiceUrl = paymentServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public WebhookEvent processWebhook(String provider, String payload, String signature) {
        webhookValidator.validateSignature(signature, payload);

        WebhookEvent event = new WebhookEvent();
        event.setEventType(extractEventType(payload));
        event.setProvider(provider);
        event.setPayload(payload);
        event.setStatus(WebhookStatus.RECEIVED);
        event = webhookEventRepository.save(event);

        boolean success = deliverWebhookWithRetry(event);

        if (success) {
            event.setStatus(WebhookStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
            log.info("Successfully processed webhook event {} for provider {}", event.getId(), provider);
        } else {
            event.setStatus(WebhookStatus.PENDING_RETRY);
            webhookEventRepository.save(event);
            log.warn("Webhook event {} failed after {} attempts, marked for retry", event.getId(), MAX_RETRY_ATTEMPTS);
        }

        return event;
    }

    private boolean deliverWebhookWithRetry(WebhookEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                event.setStatus(WebhookStatus.PROCESSING);
                event.setAttempt(attempt);
                webhookEventRepository.save(event);

                forwardToPaymentService(event);

                return true;
            } catch (RestClientException e) {
                log.warn("Webhook delivery attempt {} failed for event {}: {}", attempt, event.getId(), e.getMessage());
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return false;
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
