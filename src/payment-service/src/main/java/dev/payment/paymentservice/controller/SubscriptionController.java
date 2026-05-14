package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.ApiResponse;
import dev.payment.paymentservice.dto.SubscriptionRequest;
import dev.payment.paymentservice.entity.Subscription;
import dev.payment.paymentservice.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/subscriptions", "/api/v1/subscriptions"})
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    private String getMerchantId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof String s ? s : "merchant_default";
    }

    @PostMapping
    @Operation(summary = "Create a subscription")
    public ResponseEntity<ApiResponse<Subscription>> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        Subscription sub = subscriptionService.createSubscription(request, getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @GetMapping
    @Operation(summary = "List merchant subscriptions")
    public ResponseEntity<ApiResponse<List<Subscription>>> listSubscriptions() {
        List<Subscription> subs = subscriptionService.getMerchantSubscriptions(getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(subs));
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get subscription details")
    public ResponseEntity<ApiResponse<Subscription>> getSubscription(@PathVariable String subscriptionId) {
        Subscription sub = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel a subscription")
    public ResponseEntity<ApiResponse<Subscription>> cancelSubscription(@PathVariable String subscriptionId) {
        Subscription sub = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @PostMapping("/{subscriptionId}/pause")
    @Operation(summary = "Pause a subscription")
    public ResponseEntity<ApiResponse<Subscription>> pauseSubscription(@PathVariable String subscriptionId) {
        Subscription sub = subscriptionService.pauseSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @PostMapping("/{subscriptionId}/resume")
    @Operation(summary = "Resume a paused subscription")
    public ResponseEntity<ApiResponse<Subscription>> resumeSubscription(@PathVariable String subscriptionId) {
        Subscription sub = subscriptionService.resumeSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }
}
