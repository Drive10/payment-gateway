package dev.payment.simulator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class SimulatorService {
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    private static final String[] OUTCOMES = {"CAPTURED", "AUTHORIZED", "CHALLENGE_REQUIRED", "FAILED"};
    private static final String[] FAILURE_REASONS = {
        "Insufficient funds", "Card declined", "Invalid card", "Expired card", "Processing error"
    };

    public SimulatorService(RestTemplate restTemplate,
                            @Value("${app.payment-service.url}") String paymentServiceUrl,
                            @Value("${app.webhook.secret}") String webhookSecret,
                            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"payment.payment_created", "payment.payment_status_updated"}, groupId = "simulator-group")
    public void processPaymentEvent(String payload) {
        try {
            log.info("Raw payload: {}", payload);
            JsonNode data = objectMapper.readTree(payload);
            String paymentId = data.has("paymentId") ? data.get("paymentId").asText() : "";
            String eventType = data.has("eventType") ? data.get("eventType").asText()
                : data.has("newStatus") ? data.get("newStatus").asText() : "unknown";
            log.info("Parsed event: {} for payment {} (fields: {})", eventType, paymentId, data.fieldNames());

            if (paymentId.isEmpty()) return;

            Thread.sleep(500 + random.nextInt(2000));

            String outcome = "payment." + getWeightedOutcome();
            String callbackPayload = objectMapper.writeValueAsString(Map.of(
                "eventType", outcome,
                "paymentId", paymentId
            ));
            String signature = computeHmac(callbackPayload, webhookSecret);
            String webhookId = UUID.randomUUID().toString();

            log.info("Callback: {} -> {} via webhook {}", paymentId, outcome, webhookId);
            restTemplate.postForEntity(
                paymentServiceUrl + "/api/payments/webhooks/provider",
                createWebhookRequest(callbackPayload, webhookId, signature),
                String.class
            );
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage());
        }
    }

    private String getWeightedOutcome() {
        int roll = random.nextInt(100);
        if (roll < 60) return "captured";
        if (roll < 80) return "authorized";
        if (roll < 90) return "failed";
        return "challenge_required";
    }

    private org.springframework.http.HttpEntity<String> createWebhookRequest(String payload, String webhookId, String signature) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Webhook-Id", webhookId);
        headers.set("X-Webhook-Signature", signature);
        headers.set("Content-Type", "application/json");
        return new org.springframework.http.HttpEntity<>(payload, headers);
    }

    private String computeHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    public String simulatePayment(String paymentId) {
        try {
            Thread.sleep(1000 + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (random.nextInt(10) < 8) return "CAPTURED";
        else if (random.nextInt(10) < 5) return "FAILED";
        else return "CHALLENGE_REQUIRED";
    }
}
