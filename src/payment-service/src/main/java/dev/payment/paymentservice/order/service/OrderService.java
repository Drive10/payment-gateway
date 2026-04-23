package dev.payment.paymentservice.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.order.dto.CreateOrderRequest;
import dev.payment.paymentservice.order.dto.OrderResponse;
import dev.payment.paymentservice.order.entity.Order;
import dev.payment.paymentservice.order.entity.OrderStatus;
import dev.payment.paymentservice.order.exception.OrderException;
import dev.payment.paymentservice.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private static final UUID DEFAULT_MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OrderStateMachine orderStateMachine;

    public OrderService(OrderRepository orderRepository, ObjectMapper objectMapper, 
                     OrderStateMachine orderStateMachine) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.orderStateMachine = orderStateMachine;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.isBlank()) {
            throw new OrderException("Authentication required to create an order");
        }

        Order order = new Order();
        order.setUserId(generateUserId(authenticatedUser));
        order.setOrderReference(generateOrderReference());
        order.setExternalReference(resolveExternalReference(request.externalReference()));
        order.setMerchantId(DEFAULT_MERCHANT_ID.toString());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());
        order.setDescription(request.description());
        order.setCustomerEmail(resolveCustomerEmail(request.customerEmail(), authenticatedUser));
        order.setStatus(OrderStatus.PENDING);
        
        if (request.metadata() != null) {
            try {
                order.setMetadata(objectMapper.writeValueAsString(request.metadata()));
            } catch (JsonProcessingException e) {
                throw new OrderException("Failed to serialize metadata");
            }
        }

        Order savedOrder = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(savedOrder);
        
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, String authenticatedUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String authenticatedUser, OrderStatus status, Pageable pageable) {
        UUID userId = generateUserId(authenticatedUser);
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            orders = orderRepository.findByUserId(userId, pageable);
        }
        return orders.map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        
        orderEventPublisher.publishOrderStatusChanged(savedOrder, oldStatus, newStatus);
        
        return toResponse(savedOrder);
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

    private String generateOrderReference() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String resolveExternalReference(String externalReference) {
        if (externalReference != null && !externalReference.isBlank()) {
            return externalReference;
        }
        return "EXT-" + Instant.now().toEpochMilli();
    }

    private UUID generateUserId(String username) {
        if (username == null || username.isBlank()) {
            throw new OrderException("User authentication required");
        }
        return UUID.nameUUIDFromBytes(username.trim().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String resolveCustomerEmail(String requestedCustomerEmail, String authenticatedUser) {
        if (requestedCustomerEmail != null && !requestedCustomerEmail.isBlank()) {
            return requestedCustomerEmail;
        }
        if (authenticatedUser != null && !authenticatedUser.isBlank()) {
            return authenticatedUser.trim().toLowerCase();
        }
        return null;
    }
}
