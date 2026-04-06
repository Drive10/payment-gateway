package dev.payment.paymentservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.dto.request.CreatePaymentLinkRequest;
import dev.payment.paymentservice.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.dto.request.CreateRefundRequest;
import dev.payment.paymentservice.dto.request.InitiatePaymentRequest;
import dev.payment.paymentservice.dto.request.VerifyOtpRequest;
import dev.payment.paymentservice.dto.response.PaymentLinkResponse;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.dto.response.RefundResponse;
import dev.payment.paymentservice.dto.response.MerchantBalanceResponse;
import dev.payment.paymentservice.dto.response.PaymentDetailResponse;
import dev.payment.paymentservice.dto.response.InitiatePaymentResponse;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.service.AuthService;
import dev.payment.paymentservice.service.LedgerService;
import dev.payment.paymentservice.service.PaymentLinkService;
import dev.payment.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentLinkService paymentLinkService;
    private final AuthService authService;
    private final LedgerService ledgerService;

    public PaymentController(PaymentService paymentService, PaymentLinkService paymentLinkService, AuthService authService, LedgerService ledgerService) {
        this.paymentService = paymentService;
        this.paymentLinkService = paymentLinkService;
        this.authService = authService;
        this.ledgerService = ledgerService;
    }

    // ===== Payment Link Endpoints (Authenticated - Dashboard) =====

    @PostMapping("/links")
    public ApiResponse<PaymentLinkResponse> createPaymentLink(
            @Valid @RequestBody CreatePaymentLinkRequest request,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentLinkService.createPaymentLink(request, actor));
    }

    @GetMapping("/links")
    public ApiResponse<java.util.List<PaymentLinkResponse>> getPaymentLinks(
            @RequestParam UUID merchantId,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentLinkService.getMerchantPaymentLinks(merchantId));
    }

    // ===== Public Payment Link Endpoint (Frontend) =====

    @GetMapping("/link/{referenceId}")
    public ApiResponse<PaymentLinkResponse> getPublicPaymentLink(@PathVariable String referenceId) {
        return ApiResponse.success(paymentLinkService.getPaymentLink(referenceId));
    }

    // ===== Existing Payment Endpoints =====

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.createPayment(request, idempotencyKey, actor, false));
    }

    @PostMapping("/{paymentId}/capture")
    public ApiResponse<PaymentResponse> capturePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CapturePaymentRequest request,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.capturePayment(paymentId, request, actor, false));
    }

    @PostMapping("/{paymentId}/refunds")
    public ApiResponse<RefundResponse> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.refundPayment(paymentId, request, idempotencyKey, actor, false));
    }

    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<PaymentResponse> payments = paymentService.getPayments(actor, status, pageable, false);
        return ApiResponse.success(PageResponse.from(payments));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable UUID paymentId, Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.getPayment(paymentId, actor, false));
    }

    @GetMapping("/{paymentId}/detail")
    public ApiResponse<PaymentDetailResponse> getPaymentDetail(@PathVariable UUID paymentId, Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.getPaymentDetail(paymentId, actor, false));
    }

    @GetMapping("/balance/{merchantId}")
    public ApiResponse<MerchantBalanceResponse> getMerchantBalance(@PathVariable UUID merchantId) {
        java.math.BigDecimal available = ledgerService.getMerchantBalance(merchantId);
        java.math.BigDecimal pending = ledgerService.getMerchantReceivable(merchantId);
        java.math.BigDecimal total = available.add(pending);
        return ApiResponse.success(new MerchantBalanceResponse(available, pending, total, "INR"));
    }

    @GetMapping("/analytics/{merchantId}")
    public ApiResponse<Map<String, Object>> getMerchantAnalytics(@PathVariable UUID merchantId) {
        return ApiResponse.success(paymentService.getMerchantAnalytics(merchantId));
    }

    @GetMapping("/analytics/{merchantId}/trends")
    public ApiResponse<List<Map<String, Object>>> getPaymentTrends(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ApiResponse.success(paymentService.getPaymentTrends(merchantId, days));
    }

    // ===== Frontend Payment Endpoints (aligned with React payment page) =====

    @PostMapping("/initiate")
    public ApiResponse<InitiatePaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        // TODO: Implement payment initiation with card/UPI/netbanking
        // This should handle card token, UPI ID, or bank code
        return ApiResponse.success(InitiatePaymentResponse.pending("txn_" + System.currentTimeMillis()));
    }

    @PostMapping("/initiate-compat")
    public ApiResponse<InitiatePaymentResponse> initiatePaymentCompat(
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        return ApiResponse.success(InitiatePaymentResponse.pending("txn_" + System.currentTimeMillis()));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<InitiatePaymentResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        // TODO: Implement OTP verification
        return ApiResponse.success(InitiatePaymentResponse.completed(request.transactionId()));
    }

    @GetMapping("/{transactionId}/status")
    public ApiResponse<InitiatePaymentResponse> getPaymentStatus(
            @PathVariable String transactionId
    ) {
        // TODO: Implement status polling
        return ApiResponse.success(InitiatePaymentResponse.pending(transactionId));
    }
}
