package dev.payment.combinedservice.payment.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.combinedservice.payment.domain.Dispute;
import dev.payment.combinedservice.payment.domain.FeeConfig;
import dev.payment.combinedservice.payment.domain.User;
import dev.payment.combinedservice.payment.domain.enums.OrderStatus;
import dev.payment.combinedservice.payment.domain.enums.PaymentStatus;
import dev.payment.combinedservice.payment.dto.request.CreateFeeConfigRequest;
import dev.payment.combinedservice.payment.dto.response.AuditLogResponse;
import dev.payment.combinedservice.payment.dto.response.FeeConfigResponse;
import dev.payment.combinedservice.payment.dto.response.PaymentResponse;
import dev.payment.combinedservice.payment.service.AuditService;
import dev.payment.combinedservice.payment.service.AuthService;
import dev.payment.combinedservice.payment.service.DisputeService;
import dev.payment.combinedservice.payment.service.FeeConfigService;
import dev.payment.combinedservice.payment.integration.client.OrderServiceClient;
import dev.payment.common.dto.OrderResponse;
import dev.payment.combinedservice.payment.service.PaymentQueryService;
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

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration")
public class AdminController {

    private final PaymentQueryService queryService;
    private final OrderServiceClient orderServiceClient;
    private final AuditService auditService;
    private final AuthService authService;
    private final FeeConfigService feeConfigService;
    private final DisputeService disputeService;

    public AdminController(
            PaymentQueryService queryService,
            OrderServiceClient orderServiceClient,
            AuditService auditService,
            AuthService authService,
            FeeConfigService feeConfigService,
            DisputeService disputeService
    ) {
        this.queryService = queryService;
        this.orderServiceClient = orderServiceClient;
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
        authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        var payments = queryService.findAll(status, pageable);
        return ApiResponse.success(new PageResponse<>(
                payments.getContent(),
                payments.getNumber(),
                payments.getSize(),
                payments.getTotalElements(),
                payments.getTotalPages(),
                payments.isLast()));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        UUID userId = UUID.nameUUIDFromBytes(actor.getEmail().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<OrderResponse> orders = orderServiceClient.getOrders(userId, status, pageable, true);
        return ApiResponse.success(new PageResponse<>(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isLast()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        var auditLogs = auditService.getAuditLogs(pageable);
        return ApiResponse.success(new PageResponse<>(
                auditLogs.getContent(),
                auditLogs.getNumber(),
                auditLogs.getSize(),
                auditLogs.getTotalElements(),
                auditLogs.getTotalPages(),
                auditLogs.isLast()));
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
        Page<Dispute> disputes = disputeService.getAllDisputes(disputeStatus, pageable);
        return ApiResponse.success(new PageResponse<>(
                disputes.getContent(),
                disputes.getNumber(),
                disputes.getSize(),
                disputes.getTotalElements(),
                disputes.getTotalPages(),
                disputes.isLast()));
    }

    @PostMapping("/disputes/{disputeId}/accept")
    public ApiResponse<Dispute> acceptDispute(
            @PathVariable UUID disputeId,
            @RequestBody @Valid Map<String, String> request,
            Authentication authentication
    ) {
        authService.getCurrentUser(authentication.getName());
        String notes = request.get("notes");
        return ApiResponse.success(disputeService.acceptDispute(disputeId, notes));
    }

    @PostMapping("/disputes/{disputeId}/reject")
    public ApiResponse<Dispute> rejectDispute(
            @PathVariable UUID disputeId,
            @RequestBody @Valid Map<String, String> request,
            Authentication authentication
    ) {
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
