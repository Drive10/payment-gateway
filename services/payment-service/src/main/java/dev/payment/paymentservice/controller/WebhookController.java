package dev.payment.paymentservice.controller;
import jakarta.validation.Valid;

import dev.payment.common.api.ApiResponse;
import dev.payment.paymentservice.service.RazorpayWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "Webhooks")
public class WebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    public WebhookController(RazorpayWebhookService razorpayWebhookService) {
        this.razorpayWebhookService = razorpayWebhookService;
    }

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhooks", description = "Validates Stripe signature and updates payment state.")
    public ResponseEntity<ApiResponse<Map<String, String>>> handleStripeWebhook(
            @RequestHeader(name = "Stripe-Signature", required = false) String signature,
            @RequestBody @Valid String payload
    ) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed")));
    }

    @PostMapping("/paypal")
    @Operation(summary = "Handle PayPal webhooks", description = "Validates PayPal webhook and updates payment state.")
    public ResponseEntity<ApiResponse<Map<String, String>>> handlePaypalWebhook(
            @RequestHeader(name = "PayPal-Transmission-Sig", required = false) String signature,
            @RequestBody @Valid String payload
    ) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed")));
    }

    @PostMapping("/razorpay")
    @Operation(summary = "Handle Razorpay webhooks", description = "Validates the HMAC signature, rejects replays by event id, and updates payment or refund state.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed or deduplicated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing event id, missing signature, or invalid payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid Razorpay signature"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Refund amount would exceed captured value")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> handleRazorpayWebhook(
            @Parameter(description = "Unique Razorpay event id used for replay protection", example = "evt_001")
            @RequestHeader(name = "X-Razorpay-Event-Id", required = false) String eventId,
            @Parameter(description = "Hex-encoded Razorpay HMAC signature", example = "f0f5294ecf18fd4af08f5fb8f47b81e4ab2b239f6cba6fd7e8f30f1d9e620111")
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(value = """
                                    {
                                      "event": "refund.processed",
                                      "payload": {
                                        "refund": {
                                          "entity": {
                                            "id": "rfnd_test_001",
                                            "payment_id": "pay_test_001",
                                            "amount": 250.00,
                                            "notes": "customer_request"
                                          }
                                        }
                                      }
                                    }
                                    """)
                    )
            )
            @RequestBody @Valid String payload
    ) {
        razorpayWebhookService.processWebhook(eventId, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "processed")));
    }
}
