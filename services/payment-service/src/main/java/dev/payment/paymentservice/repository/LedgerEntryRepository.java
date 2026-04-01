package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.ledger.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    List<LedgerEntry> findByTransactionId(String transactionId);
    
    Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.accountId = :accountId AND le.createdAt BETWEEN :startDate AND :endDate")
    List<LedgerEntry> findByAccountIdAndDateRange(UUID accountId, java.time.Instant startDate, java.time.Instant endDate);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.referenceType = :referenceType AND le.referenceId = :referenceId")
    List<LedgerEntry> findByReference(String referenceType, UUID referenceId);
}