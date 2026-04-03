package dev.payment.paymentservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.dto.request.CreateOrderRequest;
import dev.payment.paymentservice.dto.response.OrderResponse;
import dev.payment.paymentservice.service.AuthService;
import dev.payment.paymentservice.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        User actor = authService.getCurrentUser(authentication.getName());
        return ApiResponse.success(orderService.createOrder(request, actor));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        User actor = authService.getCurrentUser(authentication.getName());
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<OrderResponse> orders = orderService.getOrders(actor, status, pageable, false);
        return ApiResponse.success(PageResponse.from(orders));
    }
}
