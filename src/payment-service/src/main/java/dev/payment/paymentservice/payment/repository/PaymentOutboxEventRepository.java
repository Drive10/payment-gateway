package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.payment.domain.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {
    List<PaymentOutboxEvent> findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(OutboxEventStatus status, Instant dueAt);
}
