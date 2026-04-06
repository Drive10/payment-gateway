package dev.payment.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.orderservice.dto.CreateOrderRequest;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.orderservice.entity.Order;
import dev.payment.orderservice.entity.OrderStatus;
import dev.payment.orderservice.exception.OrderException;
import dev.payment.orderservice.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, ObjectMapper objectMapper, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());
        order.setDescription(request.description());
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
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, OrderStatus status, Pageable pageable) {
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
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getDescription(),
                order.getCreatedAt()
        );
    }
}
