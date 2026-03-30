package dev.payment.ledgerservice.repository;

import dev.payment.ledgerservice.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Optional<JournalEntry> findByReference(String reference);

    @Query("select coalesce(sum(j.amount), 0) from JournalEntry j where j.debitAccountCode = :accountCode")
    BigDecimal sumDebits(String accountCode);

    @Query("select coalesce(sum(j.amount), 0) from JournalEntry j where j.creditAccountCode = :accountCode")
    BigDecimal sumCredits(String accountCode);
}
