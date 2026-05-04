package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    boolean existsByReference(String reference);
    List<LedgerEntry> findByPaymentId(String paymentId);
}
