package dev.payment.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

            log.info("Received payment event for payment: {}", paymentId);
            
            // Webhook functionality removed as merchant onboarding logic is removed
        } catch (Exception e) {
            log.error("Error handling payment event: {}", e.getMessage());
        }
    }
}
