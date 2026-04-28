package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.service.PaymentService;
import dev.payment.paymentservice.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/create-order")
    @Operation(summary = "Create payment order", description = "Create a new payment order")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create payment", description = "Create or get a payment")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @RequestBody Map<String, Object> request) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String orderId = (String) request.get("orderId");
        
        if (orderId != null && orderId.matches("[a-fA-F0-9-]{36}")) {
            try {
                PaymentStatusResponse existing = paymentService.getPaymentStatusById(orderId);
                CreatePaymentResponse response = new CreatePaymentResponse();
                response.setPaymentId(existing.getPaymentId());
                response.setOrderId(existing.getOrderId());
                response.setAmount(existing.getAmount());
                response.setCurrency(existing.getCurrency());
                response.setStatus(existing.getStatus());
                return ResponseEntity.ok(ApiResponse.success(response));
            } catch (Exception e) {
            }
        }
        
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setOrderId(orderId);
        Object amountObj = request.get("amount");
        if (amountObj != null) {
            orderRequest.setAmount(new BigDecimal(amountObj.toString()));
        }
        orderRequest.setCurrency((String) request.get("currency"));
        orderRequest.setPaymentMethod((String) request.get("method"));
        CreatePaymentResponse response = paymentService.createPayment(null, orderRequest, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by payment ID")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentById(@PathVariable("paymentId") String paymentId) {
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

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Capture payment", description = "Capture an authorized payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capturePayment(@PathVariable("paymentId") String paymentId) {
        paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "CAPTURED")));
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

    @PostMapping("/refund")
    @Operation(summary = "Create refund", description = "Create a refund for a payment")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @Valid @RequestBody RefundRequest request) {
        String merchantId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RefundResponse response = refundService.createRefund(request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/refund/{refundId}")
    @Operation(summary = "Get refund", description = "Get refund details by refund ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(@PathVariable("refundId") String refundId) {
        RefundResponse response = refundService.getRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}