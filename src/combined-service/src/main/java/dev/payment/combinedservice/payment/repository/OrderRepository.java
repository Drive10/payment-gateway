package dev.payment.combinedservice.payment.repository;

import dev.payment.combinedservice.payment.domain.Order;
import dev.payment.combinedservice.payment.domain.User;
import dev.payment.combinedservice.payment.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUser(User user, Pageable pageable);

    Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndUser(UUID id, User user);
}
