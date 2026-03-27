package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.dto.request.CreateOrderRequest;
import dev.payment.paymentservice.dto.response.OrderResponse;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.auditService = auditService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setExternalReference(request.externalReference());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());
        order.setDescription(request.description());
        order.setStatus(OrderStatus.CREATED);
        order.setOrderReference(generateOrderReference());
        orderRepository.save(order);

        auditService.record("ORDER_CREATED", user.getEmail(), "ORDER", order.getId().toString(), "Order created for " + order.getAmount());
        log.info("event=order_created orderId={} actor={}", order.getId(), user.getEmail());
        return toResponse(order);
    }

    public Page<OrderResponse> getOrders(User user, OrderStatus status, Pageable pageable, boolean adminView) {
        if (adminView) {
            return status == null
                    ? orderRepository.findAll(pageable).map(this::toResponse)
                    : orderRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return status == null
                ? orderRepository.findByUser(user, pageable).map(this::toResponse)
                : orderRepository.findByUserAndStatus(user, status, pageable).map(this::toResponse);
    }

    public Order getOwnedOrder(UUID orderId, User user, boolean adminView) {
        if (adminView) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        }
        return orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
    }

    public void markPaymentPending(Order order) {
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);
    }

    public void markPaid(Order order) {
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
    }

    public void markFailed(Order order) {
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);
    }

    public void markRefunded(Order order) {
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    private String generateOrderReference() {
        return "ORD_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderReference(),
                order.getExternalReference(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getDescription(),
                order.getCreatedAt()
        );
    }
}
