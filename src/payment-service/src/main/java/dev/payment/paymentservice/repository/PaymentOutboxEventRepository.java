package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {
    List<PaymentOutboxEvent> findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxEventStatus status, Instant dueAt);
}
