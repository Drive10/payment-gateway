package dev.payment.paymentservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.paymentservice.service.RazorpayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    public WebhookController(RazorpayWebhookService razorpayWebhookService) {
        this.razorpayWebhookService = razorpayWebhookService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRazorpayWebhook(
            @RequestHeader(name = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        razorpayWebhookService.processWebhook(eventId, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed")));
    }
}
