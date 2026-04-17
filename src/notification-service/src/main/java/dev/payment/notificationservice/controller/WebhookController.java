package dev.payment.notificationservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.notificationservice.entity.WebhookEvent;
import dev.payment.notificationservice.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/simulator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSimulatorWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Event-ID", required = false) String eventId,
            @RequestBody String payload
    ) {
        log.info("Received webhook from simulator: eventId={}", eventId);

        WebhookEvent event = webhookService.processWebhook("SIMULATOR", payload, signature, timestamp, eventId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "received",
                "eventId", event.getId().toString(),
                "eventType", event.getEventType()
        )));
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestBody String payload
    ) {
        log.info("Received webhook from Razorpay: eventId={}", eventId);

        WebhookEvent event = webhookService.processWebhook("RAZORPAY", payload, signature, timestamp, eventId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "received",
                "eventId", event.getId().toString(),
                "eventType", event.getEventType()
        )));
    }

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestHeader(value = "Stripe-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Stripe-Event-Id", required = false) String eventId,
            @RequestBody String payload
    ) {
        log.info("Received webhook from Stripe: eventId={}", eventId);

        WebhookEvent event = webhookService.processWebhook("STRIPE", payload, signature, timestamp, eventId);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "received",
                "eventId", event.getId().toString(),
                "eventType", event.getEventType()
        )));
    }
}
