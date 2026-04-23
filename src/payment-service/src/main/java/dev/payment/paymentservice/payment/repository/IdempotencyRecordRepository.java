package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByOperationAndActorIdAndIdempotencyKey(String operation, Long actorId, String idempotencyKey);
}
