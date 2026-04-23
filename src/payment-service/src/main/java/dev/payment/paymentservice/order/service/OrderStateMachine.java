package dev.payment.paymentservice.order.service;

import dev.payment.paymentservice.order.entity.Order;
import dev.payment.paymentservice.order.entity.OrderStatus;
import dev.payment.paymentservice.order.exception.OrderException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS;
    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.CANCELLED,
                OrderStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.FAILED,
                OrderStatus.CANCELLED,
                OrderStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(
                OrderStatus.COMPLETED,
                OrderStatus.REFUNDED,
                OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.of(
                OrderStatus.REFUNDED));
        ALLOWED_TRANSITIONS.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.EXPIRED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public void transition(Order order, OrderStatus targetStatus) {
        OrderStatus current = order.getStatus();
        if (current == targetStatus) {
            return;
        }

        EnumSet<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowedTargets.contains(targetStatus)) {
            throw new OrderException(HttpStatus.CONFLICT,
                    "INVALID_ORDER_STATE_TRANSITION",
                    "Cannot transition order from " + current + " to " + targetStatus + ". Allowed: " + allowedTargets);
        }

        OrderStatus previousStatus = current;
        order.setStatus(targetStatus);
        order.setStatusMessage(previousStatus + " -> " + targetStatus);
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        EnumSet<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        return allowed.contains(to);
    }
}