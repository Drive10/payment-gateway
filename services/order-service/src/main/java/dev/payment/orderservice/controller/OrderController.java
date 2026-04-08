package dev.payment.orderservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.orderservice.dto.CreateOrderRequest;
import dev.payment.orderservice.dto.InitiatePaymentRequest;
import dev.payment.orderservice.dto.InitiatePaymentResponse;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.service.OrderService;
import dev.payment.orderservice.service.PaymentOrchestrator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
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
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        return ApiResponse.success(orderService.createOrder(request, authenticatedUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable("id") UUID id) {
        return ApiResponse.success(orderService.getOrder(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(name = "userId", required = false) UUID userId,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        int resolvedSize = Math.min(limit != null ? limit : size, 100);
        int resolvedPage = offset != null ? Math.max(offset, 0) / Math.max(resolvedSize, 1) : Math.max(page, 0);
        UUID resolvedUserId = userId != null
                ? userId
                : (authenticatedUser != null && !authenticatedUser.isBlank()
                ? UUID.nameUUIDFromBytes(authenticatedUser.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))
                : null);

        Pageable pageable = PageRequest.of(resolvedPage, Math.max(resolvedSize, 1));
        Page<OrderResponse> orders = orderService.getUserOrders(resolvedUserId, status, pageable);
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
        return ApiResponse.success(paymentOrchestrator.initiatePayment(request));
    }
}
