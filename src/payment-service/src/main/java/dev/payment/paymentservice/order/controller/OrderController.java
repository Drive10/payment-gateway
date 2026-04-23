package dev.payment.paymentservice.order.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.order.dto.CreateOrderRequest;
import dev.payment.paymentservice.order.dto.InitiatePaymentRequest;
import dev.payment.paymentservice.order.dto.InitiatePaymentResponse;
import dev.payment.paymentservice.order.dto.OrderResponse;
import dev.payment.paymentservice.order.entity.OrderStatus;
import dev.payment.paymentservice.order.service.OrderService;
import dev.payment.paymentservice.order.service.PaymentOrchestrator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentOrchestrator paymentOrchestrator;

    public OrderController(OrderService orderService, PaymentOrchestrator paymentOrchestrator) {
        this.orderService = orderService;
        this.paymentOrchestrator = paymentOrchestrator;
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String authenticatedUser = getAuthenticatedUsername();
        return ApiResponse.success(orderService.createOrder(request, authenticatedUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable("id") UUID id) {
        return ApiResponse.success(orderService.getOrder(id, getAuthenticatedUsername()));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(name = "userId", required = false) UUID userId,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int resolvedSize = Math.min(limit != null ? limit : size, 100);
        int resolvedPage = offset != null ? Math.max(offset, 0) / Math.max(resolvedSize, 1) : Math.max(page, 0);

        Pageable pageable = PageRequest.of(resolvedPage, Math.max(resolvedSize, 1));
        String authenticatedUser = getAuthenticatedUsername();
        Page<OrderResponse> orders = userId != null
                ? orderService.getOrders(userId.toString(), status, pageable)
                : orderService.getUserOrders(authenticatedUser, status, pageable);
        return ApiResponse.success(new PageResponse<>(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isLast()));
    }

    @PostMapping("/{id}/initiate-payment")
    public ApiResponse<InitiatePaymentResponse> initiatePayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        // Use orderId from path parameter
        InitiatePaymentRequest updatedRequest = new InitiatePaymentRequest(
                id,
                request.amount(),
                request.currency(),
                request.returnUrl()
        );
        return ApiResponse.success(paymentOrchestrator.initiatePayment(updatedRequest));
    }

    @PatchMapping("/{orderReference}/status")
    public ApiResponse<OrderResponse> updateOrderStatusByReference(
            @PathVariable("orderReference") String orderReference,
            @RequestBody java.util.Map<String, String> body
    ) {
        String requestedStatus = body.get("status");
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new dev.payment.paymentservice.order.exception.OrderException(
                    "status is required",
                    "MISSING_STATUS",
                    org.springframework.http.HttpStatus.BAD_REQUEST.value());
        }
        OrderStatus status = OrderStatus.valueOf(requestedStatus.toUpperCase());
        return ApiResponse.success(orderService.updateOrderStatusByReference(orderReference, status));
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        return null;
    }
}
