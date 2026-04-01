package dev.payment.paymentservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.domain.Dispute;
import dev.payment.paymentservice.domain.FeeConfig;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.dto.request.CreateFeeConfigRequest;
import dev.payment.paymentservice.dto.response.AuditLogResponse;
import dev.payment.paymentservice.dto.response.FeeConfigResponse;
import dev.payment.paymentservice.dto.response.OrderResponse;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.service.AuditService;
import dev.payment.paymentservice.service.AuthService;
import dev.payment.paymentservice.service.DisputeService;
import dev.payment.paymentservice.service.FeeConfigService;
import dev.payment.paymentservice.service.OrderService;
import dev.payment.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration")
public class AdminController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final AuditService auditService;
    private final AuthService authService;
    private final FeeConfigService feeConfigService;
    private final DisputeService disputeService;

    public AdminController(PaymentService paymentService, OrderService orderService, AuditService auditService, AuthService authService, FeeConfigService feeConfigService, DisputeService disputeService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.auditService = auditService;
        this.authService = authService;
        this.feeConfigService = feeConfigService;
        this.disputeService = disputeService;
    }

    @GetMapping("/payments")
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.success(PageResponse.from(paymentService.getPayments(actor, status, pageable, true)));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.success(PageResponse.from(orderService.getOrders(actor, status, pageable, true)));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.success(PageResponse.from(auditService.getAuditLogs(pageable)));
    }

    @PostMapping("/fee-configs")
    public ApiResponse<FeeConfigResponse> createFeeConfig(
            @Valid @RequestBody CreateFeeConfigRequest request,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        FeeConfig config = feeConfigService.createOrUpdateFeeConfig(request);
        return ApiResponse.success(toFeeConfigResponse(config));
    }

    @GetMapping("/fee-configs")
    public ApiResponse<List<FeeConfigResponse>> getFeeConfigs(Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        List<FeeConfigResponse> configs = feeConfigService.getAllFeeConfigs().stream()
                .map(this::toFeeConfigResponse)
                .toList();
        return ApiResponse.success(configs);
    }

    @GetMapping("/fee-configs/{merchantId}")
    public ApiResponse<FeeConfigResponse> getFeeConfig(@PathVariable UUID merchantId, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        return feeConfigService.getFeeConfig(merchantId)
                .map(config -> ApiResponse.success(toFeeConfigResponse(config)))
                .orElseGet(() -> ApiResponse.failure(new dev.payment.common.api.ErrorDetails("NOT_FOUND", "Fee config not found", null)));
    }

    @DeleteMapping("/fee-configs/{merchantId}")
    public ApiResponse<Void> deactivateFeeConfig(@PathVariable UUID merchantId, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        feeConfigService.deactivateFeeConfig(merchantId);
        return ApiResponse.success(null);
    }

    @GetMapping("/disputes")
    public ApiResponse<PageResponse<Dispute>> getDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Dispute.DisputeStatus disputeStatus = status != null ? Dispute.DisputeStatus.valueOf(status) : null;
        Page<Dispute> disputes = disputeService.getMerchantDisputes(null, disputeStatus, pageable);
        return ApiResponse.success(PageResponse.from(disputes));
    }

    @PostMapping("/disputes/{disputeId}/accept")
    public ApiResponse<Dispute> acceptDispute(@PathVariable UUID disputeId, @RequestBody Map<String, String> request, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        String notes = request.get("notes");
        return ApiResponse.success(disputeService.acceptDispute(disputeId, notes));
    }

    @PostMapping("/disputes/{disputeId}/reject")
    public ApiResponse<Dispute> rejectDispute(@PathVariable UUID disputeId, @RequestBody Map<String, String> request, Authentication authentication) {
        authService.getCurrentUser(authentication.getName());
        String notes = request.get("notes");
        return ApiResponse.success(disputeService.rejectDispute(disputeId, notes));
    }

    private FeeConfigResponse toFeeConfigResponse(FeeConfig config) {
        return new FeeConfigResponse(
                config.getId(),
                config.getMerchantId(),
                config.getPricingTier(),
                config.getPlatformFeePercent(),
                config.getPlatformFixedFee(),
                config.getGatewayFeePercent(),
                config.getGatewayFixedFee(),
                config.getVolumeThreshold(),
                config.getVolumeDiscountPercent(),
                config.getMinFee(),
                config.getMaxFeePercent(),
                config.isActive(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
