package dev.payment.settlementservice.repository;

import dev.payment.settlementservice.domain.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {
    Optional<SettlementBatch> findByBatchReference(String batchReference);
}
