package dev.payment.orderservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/platform/order")
public class PlatformOrderController {

    private final OrderService orderService;

    public PlatformOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PutMapping("/orders/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status
    ) {
        return ApiResponse.success(orderService.updateOrderStatus(orderId, status));
    }
}
