package dev.payment.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.notificationservice.client.MerchantClient;
import dev.payment.notificationservice.dto.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final RestTemplate restTemplate;
    private final MerchantClient merchantClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"payment.payment_created", "payment.payment_status_updated"}, groupId = "notification-group")
    public void handlePaymentEvent(String message) {
        try {
            log.info("Raw message received: {}", message);
            
            // Handle double-encoded JSON - strips outer quotes if present
            String jsonToParse = message;
            if (message != null && message.startsWith("\"") && message.endsWith("\"")) {
                jsonToParse = message.substring(1, message.length() - 1);
                jsonToParse = jsonToParse.replace("\\\"", "\"");
            }
            
            Map<String, Object> eventData = objectMapper.readValue(jsonToParse, Map.class);
            String paymentId = (String) eventData.get("paymentId");
            String merchantId = (String) eventData.get("merchantId");

            log.info("Received payment event for payment: {}, merchant: {}", paymentId, merchantId);
            
            sendWebhook(merchantId, eventData);
        } catch (Exception e) {
            log.error("Error handling payment event: {}", e.getMessage());
        }
    }

    private void sendWebhook(String merchantId, Map<String, Object> eventData) {
        String webhookUrl = merchantClient.getMerchantWebhookUrl(merchantId);
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("No webhook URL configured for merchant: {}", merchantId);
            return;
        }

        WebhookEvent event = WebhookEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("payment.updated")
            .payload(eventData)
            .timestamp(Instant.now().toEpochMilli())
            .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<WebhookEvent> entity = new HttpEntity<>(event, headers);

            log.info("Sending webhook to {}: {}", webhookUrl, event.getEventId());
            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.info("Webhook delivered successfully: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to deliver webhook to {}: {}", webhookUrl, e.getMessage());
            // In a production system, we would move this to a retry queue / DLQ
        }
    }
}
