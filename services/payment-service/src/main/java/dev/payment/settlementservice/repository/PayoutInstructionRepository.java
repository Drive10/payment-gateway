package dev.payment.settlementservice.repository;

import dev.payment.settlementservice.domain.PayoutInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutInstructionRepository extends JpaRepository<PayoutInstruction, UUID> {
    List<PayoutInstruction> findByBatchReference(String batchReference);
}
