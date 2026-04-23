package dev.payment.paymentservice.payment.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.paymentservice.payment.service.PayPalWebhookService;
import dev.payment.paymentservice.payment.service.RazorpayWebhookService;
import dev.payment.paymentservice.payment.service.StripeWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Provider webhook handlers for payment status updates")
public class WebhookController {

    private final StripeWebhookService stripeWebhookService;
    private final PayPalWebhookService paypalWebhookService;
    private final RazorpayWebhookService razorpayWebhookService;

    public WebhookController(
            StripeWebhookService stripeWebhookService,
            PayPalWebhookService paypalWebhookService,
            RazorpayWebhookService razorpayWebhookService
    ) {
        this.stripeWebhookService = stripeWebhookService;
        this.paypalWebhookService = paypalWebhookService;
        this.razorpayWebhookService = razorpayWebhookService;
    }

    @PostMapping("/stripe")
    @Operation(
            summary = "Handle Stripe webhooks",
            description = "Validates Stripe signature, processes payment events (payment_intent.succeeded, payment_intent.payment_failed, charge.refunded), and updates payment state idempotently."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed or deduplicated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing event id, missing signature, or invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid Stripe signature"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Webhook secret not configured")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> handleStripeWebhook(
            @Parameter(description = "Unique Stripe event ID", example = "evt_3MqLGmKZuxHL4cvZ0fT1x7XK")
            @RequestHeader(name = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        String eventId = extractStripeEventId(payload);
        stripeWebhookService.processWebhook(eventId, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed", "event_id", eventId)));
    }

    @PostMapping("/paypal")
    @Operation(
            summary = "Handle PayPal webhooks",
            description = "Validates PayPal webhook, processes payment capture events, and updates payment state idempotently."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed or deduplicated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing headers or invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "PayPal webhook ID not configured")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> handlePaypalWebhook(
            @Parameter(description = "Unique PayPal webhook event ID", example = "WH-8BS59582N5087534L-6N9U51WV27535243N")
            @RequestHeader(name = "PayPal-Transmission-Id", required = false) String transmissionId,
            @Parameter(description = "PayPal webhook signature", example = "XxYz123...")
            @RequestHeader(name = "PayPal-Transmission-Sig", required = false) String signature,
            @Parameter(description = "PayPal cert URL", example = "https://api.paypal.com/v1/notifications/certs/CERT-360...")
            @RequestHeader(name = "PayPal-Cert-Url", required = false) String certUrl,
            @RequestBody String payload
    ) {
        paypalWebhookService.processWebhook(transmissionId, transmissionId, signature, certUrl, payload);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed", "event_id", transmissionId)));
    }

    @PostMapping("/razorpay")
    @Operation(
            summary = "Handle Razorpay webhooks",
            description = "Validates the HMAC signature, rejects replays by event id, and updates payment or refund state."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed or deduplicated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing event id, missing signature, or invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid Razorpay signature"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Refund amount would exceed captured value"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Webhook secret not configured")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRazorpayWebhook(
            @Parameter(description = "Unique Razorpay event id used for replay protection", example = "evt_001")
            @RequestHeader(name = "X-Razorpay-Event-Id", required = false) String eventId,
            @Parameter(description = "Hex-encoded Razorpay HMAC signature", example = "f0f5294ecf18fd4af08f5fb8f47b81e4ab2b239f6cba6fd7e8f30f1d9e620111")
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        razorpayWebhookService.processWebhook(eventId, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed", "event_id", eventId != null ? eventId : "unknown")));
    }

    private String extractStripeEventId(String payload) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(payload);
            return node.has("id") ? node.get("id").asText() : "unknown_" + System.currentTimeMillis();
        } catch (Exception e) {
            return "unknown_" + System.currentTimeMillis();
        }
    }
}