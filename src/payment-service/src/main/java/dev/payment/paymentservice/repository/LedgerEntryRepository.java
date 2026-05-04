package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.LedgerEntry;
import dev.payment.paymentservice.entity.LedgerEntry.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountId = :accountId AND le.accountType = :accountType AND le.entryType = 'DEBIT' AND le.postedAt IS NOT NULL")
    BigDecimal sumDebitsByAccount(String accountId, AccountType accountType);
    
    @Query("SELECT SUM(le.amount) FROM LedgerEntry le WHERE le.accountId = :accountId AND le.accountType = :accountType AND le.entryType = 'CREDIT' AND le.postedAt IS NOT NULL")
    BigDecimal sumCreditsByAccount(String accountId, AccountType accountType);
    
    @Query("SELECT le.balanceAfter FROM LedgerEntry le WHERE le.accountId = :accountId AND le.accountType = :accountType AND le.postedAt IS NOT NULL ORDER BY le.postedAt DESC LIMIT 1")
    BigDecimal getLatestBalance(String accountId, AccountType accountType);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.journalId = :journalId ORDER BY le.createdAt ASC")
    List<LedgerEntry> findByJournalId(UUID journalId);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.paymentId = :paymentId ORDER BY le.createdAt ASC")
    List<LedgerEntry> findByPaymentId(String paymentId);
    
    @Query("SELECT le FROM LedgerEntry le WHERE le.merchantId = :merchantId AND le.postedAt IS NOT NULL ORDER BY le.postedAt DESC")
    List<LedgerEntry> findByMerchantId(String merchantId);
    
    @Query("SELECT SUM(CASE WHEN le.entryType = 'DEBIT' THEN le.amount ELSE -le.amount END) FROM LedgerEntry le WHERE le.merchantId = :merchantId AND le.postedAt IS NOT NULL")
    BigDecimal getMerchantBalance(String merchantId);
    
    boolean existsByReference(String reference);
}