package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Merchant-Id", defaultValue = "default") String merchantId) {
        try {
            CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable("orderId") String orderId) {
        try {
            PaymentStatusResponse response = paymentService.getPaymentStatus(orderId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<Map<String, Object>> capturePayment(@PathVariable("paymentId") String paymentId) {
        try {
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZED);
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("status", "CAPTURED")
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<Map<String, Object>> authorizePayment(@PathVariable("paymentId") String paymentId) {
        try {
            paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZATION_PENDING);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("status", "AUTHORIZATION_PENDING")
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/{paymentId}/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @PathVariable("paymentId") String paymentId,
            @RequestBody Map<String, String> request) {
        try {
            paymentService.verifyOtp(paymentId, request.get("otp"));
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of("status", "AUTHORIZED")
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}