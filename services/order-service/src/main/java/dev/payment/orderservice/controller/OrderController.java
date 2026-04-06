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
        return ApiResponse.success(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.success(orderService.getOrder(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<OrderResponse> orders = orderService.getUserOrders(userId, status, pageable);
        return ApiResponse.success(PageResponse.from(orders));
    }

    @PostMapping("/{id}/initiate-payment")
    public ApiResponse<InitiatePaymentResponse> initiatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        return ApiResponse.success(paymentOrchestrator.initiatePayment(request));
    }
}
