package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.WebhookDelivery;
import dev.payment.paymentservice.domain.enums.WebhookStatus;
import dev.payment.paymentservice.repository.WebhookDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final RestTemplate restTemplate;
    
    @Value("${application.webhook.base-url:http://notification-service:8084}")
    private String webhookBaseUrl;

    @Value("${application.webhook.max-retries:3}")
    private int maxRetries;

    @Value("${application.webhook.timeout-ms:5000}")
    private int timeoutMs;

    public WebhookService(WebhookDeliveryRepository webhookDeliveryRepository) {
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void sendAsyncWebhook(UUID paymentId) {
        try {
            sendWebhook(paymentId, "PAYMENT_COMPLETED");
        } catch (Exception e) {
            log.error("Failed to send async webhook for payment {}: {}", paymentId, e.getMessage());
        }
    }

    @Async
    public void sendCompensationWebhook(UUID paymentId) {
        try {
            sendWebhook(paymentId, "PAYMENT_COMPENSATED");
        } catch (Exception e) {
            log.error("Failed to send compensation webhook for payment {}: {}", paymentId, e.getMessage());
        }
    }

    public WebhookDelivery sendWebhook(UUID paymentId, String eventType) {
        String webhookUrl = webhookBaseUrl + "/api/v1/webhooks/deliver";
        
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setPaymentId(paymentId);
        delivery.setEventType(eventType);
        delivery.setStatus(WebhookStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextAttemptAt(LocalDateTime.now());
        delivery.setCreatedAt(LocalDateTime.now());
        
        webhookDeliveryRepository.save(delivery);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Event", eventType);
        headers.set("X-Payment-ID", paymentId.toString());
        
        String payload = String.format(
            "{\"event\":\"%s\",\"paymentId\":\"%s\",\"timestamp\":\"%s\"}",
            eventType, paymentId, LocalDateTime.now()
        );
        
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus(WebhookStatus.DELIVERED);
                delivery.setDeliveredAt(LocalDateTime.now());
                log.info("Webhook delivered successfully for payment {}", paymentId);
            } else {
                delivery.setStatus(WebhookStatus.FAILED);
                delivery.setErrorMessage("HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            delivery.setStatus(WebhookStatus.RETRYING);
            delivery.setErrorMessage(e.getMessage());
            delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(5));
            log.warn("Webhook delivery failed for payment {}, will retry: {}", paymentId, e.getMessage());
        }
        
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(LocalDateTime.now());
        
        return webhookDeliveryRepository.save(delivery);
    }

    public void retryFailedWebhooks() {
        LocalDateTime now = LocalDateTime.now();
        
        webhookDeliveryRepository.findByStatusAndNextAttemptAtBefore(WebhookStatus.RETRYING, now)
            .forEach(delivery -> {
                if (delivery.getAttemptCount() < maxRetries) {
                    log.info("Retrying webhook delivery for payment {}, attempt {}", 
                             delivery.getPaymentId(), delivery.getAttemptCount() + 1);
                    sendWebhook(delivery.getPaymentId(), delivery.getEventType());
                } else {
                    delivery.setStatus(WebhookStatus.FAILED);
                    delivery.setErrorMessage("Max retries exceeded");
                    webhookDeliveryRepository.save(delivery);
                    log.error("Webhook delivery failed permanently for payment {}", delivery.getPaymentId());
                }
            });
    }
}
