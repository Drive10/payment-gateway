package dev.payment.paymentservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.dto.response.AuditLogResponse;
import dev.payment.paymentservice.dto.response.OrderResponse;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.service.AuditService;
import dev.payment.paymentservice.service.AuthService;
import dev.payment.paymentservice.service.OrderService;
import dev.payment.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration")
public class AdminController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final AuditService auditService;
    private final AuthService authService;

    public AdminController(PaymentService paymentService, OrderService orderService, AuditService auditService, AuthService authService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.auditService = auditService;
        this.authService = authService;
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
}
