package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.service.PaymentService;
import dev.payment.paymentservice.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable("orderId") String orderId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capturePayment(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "CAPTURED")));
    }

    @PostMapping("/{paymentId}/authorize-pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authorizePaymentPending(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZATION_PENDING);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZATION_PENDING")));
    }

    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authorizePayment(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZED);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZED")));
    }

    @PostMapping("/{paymentId}/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @PathVariable("paymentId") String paymentId,
            @RequestBody Map<String, String> request) {
        paymentService.verifyOtp(paymentId, request.get("otp"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZED")));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @Valid @RequestBody RefundRequest request) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RefundResponse response = refundService.createRefund(request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/refund/{refundId}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(@PathVariable("refundId") String refundId) {
        RefundResponse response = refundService.getRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}