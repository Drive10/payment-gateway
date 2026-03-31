package dev.payment.webhookservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.webhookservice.dto.WebhookPayload;
import dev.payment.webhookservice.entity.WebhookEvent;
import dev.payment.webhookservice.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
            @RequestBody String payload
    ) {
        log.info("Received webhook from simulator");

        WebhookEvent event = webhookService.processWebhook("SIMULATOR", payload, signature);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "received",
                "eventId", event.getId().toString(),
                "eventType", event.getEventType()
        )));
    }
}
