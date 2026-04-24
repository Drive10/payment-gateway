package dev.payment.notificationservice.controller;
import jakarta.validation.Valid;

import dev.payment.common.api.ApiResponse;
import dev.payment.notificationservice.dto.ForwardWebhookRequest;
import dev.payment.notificationservice.domain.entities.WebhookEvent;
import dev.payment.notificationservice.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/webhooks")
public class PlatformWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PlatformWebhookController.class);

    private final WebhookService webhookService;

    public PlatformWebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> receiveInternalWebhook(
            @RequestBody @Valid ForwardWebhookRequest request
    ) {
        log.info("Received internal webhook for event type: {}", request.eventType());

        WebhookEvent event = new WebhookEvent();
        event.setId(request.eventId());
        event.setEventType(request.eventType());
        event.setPayload(request.payload() != null ? request.payload().toString() : "{}");
        event.setProvider("INTERNAL");
        event.setStatus(dev.payment.notificationservice.domain.entities.WebhookStatus.PROCESSING);

        webhookService.forwardToPaymentService(event);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "forwarded",
                "eventId", event.getId().toString()
        )));
    }
}
