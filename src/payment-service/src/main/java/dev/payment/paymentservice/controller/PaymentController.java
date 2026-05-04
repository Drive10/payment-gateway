package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.service.PaymentService;
import dev.payment.paymentservice.service.RefundService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing API for creating and managing payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-order")
    @Operation(summary = "Create payment order", description = "Create a new payment order")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Initiate a new payment with card, UPI, or netbanking")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> initiatePayment(
            @RequestBody Map<String, Object> request) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        Object amountObj = request.get("amount");
        if (amountObj != null) {
            orderRequest.setAmount(new BigDecimal(amountObj.toString()));
        }
        orderRequest.setCurrency((String) request.get("currency"));
        orderRequest.setPaymentMethod((String) request.get("paymentMethod"));
        
        String idempotencyKey = (String) request.get("idempotencyKey");
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, orderRequest, merchantId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/intents")
    @Operation(summary = "Create payment intent", description = "Create a payment intent for a checkout session")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPaymentIntent(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/intents/{paymentId}/confirm")
    @Operation(summary = "Confirm payment intent", description = "Confirm payment method and start authorization")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> confirmPaymentIntent(
            @PathVariable String paymentId,
            @RequestBody ConfirmPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CreatePaymentResponse response = paymentService.confirmPaymentIntent(paymentId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/intents/{paymentId}/capture")
    @Operation(summary = "Capture payment intent", description = "Capture an authorized payment intent")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> capturePaymentIntent(
            @PathVariable String paymentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CreatePaymentResponse response = paymentService.capturePaymentIntent(paymentId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhooks/provider")
    @Operation(summary = "Provider webhook", description = "Consume provider callbacks with inbox dedupe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleProviderWebhook(
            @RequestHeader("X-Webhook-Id") String webhookId,
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestBody String payload) {
        if (!verifyWebhookSignature(payload, signature)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid webhook signature"));
        }

        try {
            Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            String eventType = String.valueOf(body.getOrDefault("eventType", ""));
            String paymentId = String.valueOf(body.getOrDefault("paymentId", ""));
            paymentService.processProviderWebhook(webhookId, eventType, paymentId, payload);
            return ResponseEntity.ok(ApiResponse.success(Map.of("processed", true, "webhookId", webhookId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid webhook payload"));
        }
    }

    @GetMapping("/intents/{paymentId}")
    @Operation(summary = "Get payment intent", description = "Fetch payment intent state")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> getPaymentIntent(@PathVariable String paymentId) {
        CreatePaymentResponse response = paymentService.getPaymentIntentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentById(@PathVariable("paymentId") String paymentId) {
        PaymentStatusResponse response = paymentService.getPaymentStatusById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}/status")
    @Operation(summary = "Get payment status by payment ID", description = "Retrieve payment status by payment ID")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatusByPaymentId(@PathVariable("paymentId") String paymentId) {
        PaymentStatusResponse response = paymentService.getPaymentStatusById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{orderId}")
    @Operation(summary = "Get payment status", description = "Get payment status by order ID")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(@PathVariable("orderId") String orderId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get merchant orders", description = "Get all payment orders for the merchant")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> getMerchantOrders() {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<PaymentStatusResponse> orders = paymentService.getMerchantOrders(merchantId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/list")
    @Operation(summary = "List payments", description = "List all payments with pagination")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> listPayments(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<PaymentStatusResponse> orders = paymentService.getAllPayments(limit, offset);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/balance/{merchantId}")
    @Operation(summary = "Get merchant balance", description = "Get merchant balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchantBalance(@PathVariable("merchantId") String merchantId) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "available", 0,
            "pending", 0,
            "currency", "USD"
        )));
    }

    @GetMapping("/link/{referenceId}")
    @Operation(summary = "Get payment link", description = "Get payment link details by reference ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentLink(@PathVariable("referenceId") String referenceId) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "referenceId", referenceId,
            "status", "ACTIVE",
            "amount", 0
        )));
    }

    @GetMapping("")
    @Operation(summary = "Get payments root", description = "Root endpoint - returns empty list")
    public ResponseEntity<ApiResponse<List<PaymentStatusResponse>>> getPaymentsRoot() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capture payment", description = "Capture an authorized payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capturePayment(@PathVariable("paymentId") String paymentId) {
        CreatePaymentResponse response = paymentService.capturePaymentIntent(paymentId, null);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", response.getStatus(), "paymentId", response.getPaymentId())));
    }

    @PostMapping("/{paymentId}/authorize-pending")
    @Operation(summary = "Mark payment as pending authorization", description = "Set payment status to authorization pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authorizePaymentPending(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZATION_PENDING);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZATION_PENDING")));
    }

    @PostMapping("/{paymentId}/authorize")
    @Operation(summary = "Authorize payment", description = "Authorize a payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> authorizePayment(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.AUTHORIZED);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZED")));
    }

    @PostMapping("/{paymentId}/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify OTP for payment authorization")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @PathVariable("paymentId") String paymentId,
            @RequestBody Map<String, String> request) {
        paymentService.verifyOtp(paymentId, request.get("otp"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZED")));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP (compat)", description = "Verify OTP using transaction ID in body")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtpCompat(@RequestBody VerifyOtpRequest request) {
        paymentService.verifyOtp(request.getTransactionId(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "AUTHORIZED")));
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Create refund", description = "Create a refund for a payment")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RefundResponse response = refundService.createRefund(request, merchantId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/refund/{refundId}")
    @Operation(summary = "Get refund", description = "Get refund details by refund ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(@PathVariable("refundId") String refundId) {
        RefundResponse response = refundService.getRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
