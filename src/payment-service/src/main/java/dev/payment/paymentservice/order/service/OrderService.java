package dev.payment.paymentservice.order.service;

import dev.payment.paymentservice.order.dto.CreateOrderRequest;
import dev.payment.paymentservice.order.dto.OrderResponse;
import dev.payment.paymentservice.order.entity.Order;
import dev.payment.paymentservice.order.entity.OrderStatus;
import dev.payment.paymentservice.order.exception.OrderException;
import dev.payment.paymentservice.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;

    public OrderService(OrderRepository orderRepository, OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.orderStateMachine = orderStateMachine;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String authenticatedUser) {
        Order order = new Order();
        order.setUserId(resolveUserId(request.userId(), authenticatedUser));
        order.setExternalReference(request.externalReference());
        order.setOrderReference("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());
        order.setStatus(OrderStatus.PENDING);
        order.setStatusMessage("Order created");
        order.setDescription(request.description());
        order.setCustomerEmail(request.customerEmail());
        order.setMerchantId(null);
        if (request.metadata() != null && !request.metadata().isEmpty()) {
            order.setMetadata(toMetadataString(request.metadata()));
        }

        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, String authenticatedUser) {
        Order order = findOrder(orderId);
        UUID actorId = resolveUserId(null, authenticatedUser);
        if (!order.getUserId().equals(actorId)) {
            throw new OrderException(
                    "Order does not belong to authenticated user",
                    "ORDER_FORBIDDEN",
                    HttpStatus.FORBIDDEN.value());
        }
        return toResponse(order);
    }

@Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> OrderException.notFound("Order not found: " + orderId));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByReference(String orderReference) {
        return toResponse(findOrderByReference(orderReference));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String authenticatedUser, OrderStatus status, Pageable pageable) {
        UUID actorId = resolveUserId(null, authenticatedUser);
        Page<Order> orders = status == null
                ? orderRepository.findByUserId(actorId, pageable)
                : orderRepository.findByUserIdAndStatus(actorId, status, pageable);
        return orders.map(this::toResponse);
    }

@Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(String userId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        UUID userUuid = UUID.fromString(userId);
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(userUuid, status, pageable);
        } else {
            orders = orderRepository.findByUserId(userUuid, pageable);
        }
        return orders.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserId(UUID.fromString(userId), pageable);
        return orders.map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = findOrder(orderId);
        orderStateMachine.transition(order, status);
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderStatusByReference(String orderReference, OrderStatus status) {
        Order order = findOrderByReference(orderReference);
        orderStateMachine.transition(order, status);
        order.setUpdatedAt(Instant.now());
        return toResponse(orderRepository.save(order));
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        "Order not found: " + orderId,
                        "ORDER_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));
    }

    private Order findOrderByReference(String orderReference) {
        return orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new OrderException(
                        "Order not found: " + orderReference,
                        "ORDER_NOT_FOUND",
                        HttpStatus.NOT_FOUND.value()));
    }

    private UUID resolveUserId(UUID requestedUserId, String authenticatedUser) {
        if (requestedUserId != null) {
            return requestedUserId;
        }
        if (authenticatedUser == null || authenticatedUser.isBlank()) {
            throw new OrderException(
                    "Authenticated user is required",
                    "UNAUTHENTICATED",
                    HttpStatus.UNAUTHORIZED.value());
        }
        return UUID.nameUUIDFromBytes(authenticatedUser.getBytes(StandardCharsets.UTF_8));
    }

    private String toMetadataString(Map<String, String> metadata) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            builder.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        builder.append("}");
        return builder.toString();
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getOrderReference(),
                order.getExternalReference(),
                order.getMerchantId(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getDescription(),
                order.getCreatedAt(),
                order.getCustomerEmail()
        );
    }
}
