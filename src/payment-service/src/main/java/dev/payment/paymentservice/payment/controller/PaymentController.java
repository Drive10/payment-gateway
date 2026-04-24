package dev.payment.paymentservice.payment.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.payment.domain.User;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.payment.dto.request.CardTokenizationRequest;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentLinkRequest;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.payment.dto.request.CreateRefundRequest;
import dev.payment.paymentservice.payment.dto.request.InitiatePaymentRequest;
import dev.payment.paymentservice.payment.dto.response.*;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.service.AuthService;
import dev.payment.paymentservice.payment.service.LedgerService;
import dev.payment.paymentservice.payment.service.PaymentAnalyticsService;
import dev.payment.paymentservice.payment.service.PaymentLinkService;
import dev.payment.paymentservice.payment.service.PaymentQueryService;
import dev.payment.paymentservice.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/payments", "/api/v1/payments"})
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentQueryService queryService;
    private final PaymentLinkService paymentLinkService;
    private final PaymentAnalyticsService analyticsService;
    private final AuthService authService;
    private final LedgerService ledgerService;

    public PaymentController(
            PaymentService paymentService,
            PaymentQueryService queryService,
            PaymentLinkService paymentLinkService,
            PaymentAnalyticsService analyticsService,
            AuthService authService,
            LedgerService ledgerService) {
        this.paymentService = paymentService;
        this.queryService = queryService;
        this.paymentLinkService = paymentLinkService;
        this.analyticsService = analyticsService;
        this.authService = authService;
        this.ledgerService = ledgerService;
    }

    @PostMapping("/links")
    public ApiResponse<PaymentLinkResponse> createPaymentLink(
            @Valid @RequestBody CreatePaymentLinkRequest request,
            Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentLinkService.createPaymentLink(request, actor));
    }

    @GetMapping("/links")
    public ApiResponse<java.util.List<PaymentLinkResponse>> getPaymentLinks(
            @RequestParam("merchantId") UUID merchantId,
            Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentLinkService.getMerchantPaymentLinks(merchantId));
    }

    @GetMapping("/links/{referenceId}")
    public ApiResponse<PaymentLinkResponse> getPaymentLink(
            @PathVariable("referenceId") String referenceId,
            Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentLinkService.getPaymentLink(referenceId));
    }

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        String username = authentication != null ? authentication.getName() : "anonymous";
        User actor = authService.getCurrentUser(username);
        return ApiResponse.success(paymentService.createPayment(request, idempotencyKey, actor));
    }

    @PostMapping("/{paymentId}/capture")
    public ApiResponse<PaymentResponse> capturePayment(
            @PathVariable("paymentId") UUID paymentId,
            @Valid @RequestBody CapturePaymentRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        User actor = authService.getCurrentUser(username);
        return ApiResponse.success(paymentService.capturePayment(paymentId, request, actor));
    }

    @PostMapping("/{paymentId}/verify-otp")
    public ApiResponse<PaymentResponse> verifyOtp(
            @PathVariable("paymentId") UUID paymentId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        String otp = body.get("otp");
        if (otp == null || otp.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_OTP", "OTP is required");
        }
        String username = authentication != null ? authentication.getName() : "anonymous";
        User actor = authService.getCurrentUser(username);
        return ApiResponse.success(paymentService.verifyOtp(paymentId, otp, actor));
    }

    @PostMapping("/{paymentId}/refunds")
    public ApiResponse<RefundResponse> refundPayment(
            @PathVariable("paymentId") UUID paymentId,
            @Valid @RequestBody CreateRefundRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.refundPayment(paymentId, request, idempotencyKey, actor));
    }

    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        int resolvedSize = resolveSize(limit, size);
        int resolvedPage = resolvePage(offset, page, resolvedSize);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        Page<PaymentResponse> payments = queryService.findAll(status, pageable);
        return ApiResponse.success(new PageResponse<>(
                payments.getContent(),
                payments.getNumber(),
                payments.getSize(),
                payments.getTotalElements(),
                payments.getTotalPages(),
                payments.isLast()));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable("paymentId") UUID paymentId, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(queryService.findById(paymentId));
    }

    @GetMapping("/{paymentId}/detail")
    public ApiResponse<PaymentDetailResponse> getPaymentDetail(@PathVariable("paymentId") UUID paymentId, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(queryService.findDetailById(paymentId));
    }

    @PostMapping("/tokenize")
    public ApiResponse<CardTokenizationResponse> tokenizeCard(
            @RequestBody CardTokenizationRequest request) {
        CardTokenizationResponse tokenizationResponse = paymentService.tokenizeCard(request);
        return ApiResponse.success(tokenizationResponse);
    }

    @PostMapping("/{paymentId}/retry")
    public ApiResponse<PaymentResponse> retryPayment(
            @PathVariable("paymentId") UUID paymentId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(paymentService.retryPayment(paymentId, idempotencyKey, actor));
    }

    @GetMapping("/balance/{merchantId}")
    public ApiResponse<MerchantBalanceResponse> getMerchantBalance(@PathVariable("merchantId") UUID merchantId) {
        java.math.BigDecimal available = ledgerService.getMerchantBalance(merchantId);
        java.math.BigDecimal pending = ledgerService.getMerchantReceivable(merchantId);
        java.math.BigDecimal total = available.add(pending);
        return ApiResponse.success(new MerchantBalanceResponse(available, pending, total, "INR"));
    }

    @GetMapping("/analytics/{merchantId}")
    public ApiResponse<MerchantAnalyticsResponse> getMerchantAnalytics(@PathVariable("merchantId") UUID merchantId) {
        return ApiResponse.success(analyticsService.getMerchantAnalytics(merchantId));
    }

    @GetMapping("/analytics/{merchantId}/trends")
    public ApiResponse<PaymentTrendsResponse> getPaymentTrends(
            @PathVariable("merchantId") UUID merchantId,
            @RequestParam(name = "days", defaultValue = "30") int days) {
        return ApiResponse.success(analyticsService.getPaymentTrends(merchantId, days));
    }

@PostMapping("/initiate")
    public ApiResponse<InitiatePaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : "system@payflow.dev";
        User actor = authService.getCurrentUser(userEmail);
        
        UUID orderId = request.orderId();
        
        UUID merchantId;
        if (request.merchantId() != null && !request.merchantId().isEmpty()) {
            try {
                merchantId = UUID.fromString(request.merchantId());
            } catch (Exception e) {
                merchantId = actor.getId() != null ? actor.getId() : UUID.fromString("00000000-0000-0000-0000-000000000002");
            }
        } else {
            merchantId = actor.getId() != null ? actor.getId() : UUID.fromString("00000000-0000-0000-0000-000000000002");
        }
        
        if (orderId == null) {
            orderId = UUID.randomUUID();
        }
        
        PaymentMethod paymentMethod = resolvePaymentMethod(request.paymentMethod());
        String notes = buildCheckoutNotes(request, paymentMethod);
        CreatePaymentRequest createRequest = CreatePaymentRequest.createLegacy(
                orderId,
                merchantId,
                paymentMethod,
                "RAZORPAY_SIMULATOR",
                dev.payment.paymentservice.payment.domain.enums.TransactionMode.TEST,
                notes
        );
        
        PaymentResponse paymentResponse = paymentService.createPayment(createRequest, 
                idempotencyKey != null ? idempotencyKey : "idem_" + System.currentTimeMillis(), 
                actor);
        
        InitiatePaymentResponse initiateResponse = InitiatePaymentResponse.fromStatus(
                paymentResponse.status(),
                paymentResponse.id().toString(),
                paymentResponse.checkoutUrl()
        );
        
        return ApiResponse.success(initiateResponse);
    }

    @PostMapping("/initiate-compat")
    public ApiResponse<InitiatePaymentResponse> initiatePaymentCompat(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        return initiatePayment(request, idempotencyKey, authentication);
    }

    @GetMapping("/{transactionId}/status")
    public ApiResponse<InitiatePaymentResponse> getPaymentStatus(@PathVariable("transactionId") String transactionId) {
        PaymentResponse payment = queryService.findById(UUID.fromString(transactionId));
        InitiatePaymentResponse response = InitiatePaymentResponse.fromStatus(
                payment.status(),
                payment.id().toString(),
                payment.checkoutUrl()
        );
        return ApiResponse.success(response);
    }

    private int resolveSize(Integer limit, Integer size) {
        Integer effectiveLimit = (limit != null && limit > 0) ? limit : null;
        Integer effectiveSize = effectiveLimit != null ? effectiveLimit : (size != null ? size : 10);
        return Math.max(Math.min(effectiveSize, 100), 1);
    }

    private int resolvePage(Integer offset, Integer page, int resolvedSize) {
        if (offset != null && offset >= 0) {
            return offset / Math.max(resolvedSize, 1);
        }
        Integer effectivePage = page != null ? page : 0;
        return Math.max(effectivePage, 0);
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PaymentMethod.CARD;
        }
        String normalized = paymentMethod.trim().toUpperCase();
        return switch (normalized) {
            case "UPI" -> PaymentMethod.UPI;
            case "NETBANKING", "NET_BANKING" -> PaymentMethod.NET_BANKING;
            case "WALLET" -> PaymentMethod.WALLET;
            case "CARD" -> PaymentMethod.CARD;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD",
                    "Unsupported payment method: " + paymentMethod);
        };
    }

    private String buildCheckoutNotes(InitiatePaymentRequest request, PaymentMethod paymentMethod) {
        StringBuilder notes = new StringBuilder("Initiated via checkout flow");
        if (paymentMethod == PaymentMethod.UPI && request.upiId() != null && !request.upiId().isBlank()) {
            notes.append("|UPI_ID=").append(request.upiId().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            notes.append("|EMAIL=").append(request.email().trim());
        }
        return notes.toString();
    }
}
