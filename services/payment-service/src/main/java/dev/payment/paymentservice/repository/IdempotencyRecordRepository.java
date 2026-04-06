package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByOperationAndActorIdAndIdempotencyKey(String operation, Long actorId, String idempotencyKey);
}
