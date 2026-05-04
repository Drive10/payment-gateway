package dev.payment.paymentservice.service;

import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.LedgerEntry.AccountType;
import dev.payment.paymentservice.entity.LedgerEntry.EntryType;
import dev.payment.paymentservice.entity.Journal.JournalStatus;
import dev.payment.paymentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerEntryRepository ledgerRepository;
    private final JournalRepository journalRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Journal createPaymentJournal(Payment payment) {
        String paymentId = payment.getId().toString();
        BigDecimal amount = payment.getAmount();
        String currency = payment.getCurrency();
        String merchantId = payment.getMerchantId();

        BigDecimal platformFee = payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal gatewayFee = payment.getGatewayFee() != null ? payment.getGatewayFee() : BigDecimal.ZERO;
        BigDecimal netAmount = amount.subtract(platformFee).subtract(gatewayFee);

        Journal journal = Journal.builder()
                .referenceId(paymentId)
                .referenceType("PAYMENT")
                .description("Payment captured: " + paymentId)
                .totalDebit(amount)
                .totalCredit(amount)
                .status(JournalStatus.PENDING)
                .build();
        journal = journalRepository.save(journal);

        if (payment.getStatus() == Payment.PaymentStatus.CAPTURED) {
            createEntry(journal, merchantId, AccountType.MERCHANT_RECEivable, dev.payment.paymentservice.entity.LedgerEntry.EntryType.CREDIT, amount, currency, paymentId, paymentId);
            createEntry(journal, "SYSTEM", AccountType.CUSTOMER_ESCROW, EntryType.DEBIT, amount, currency, paymentId, paymentId);
            
            createEntry(journal, merchantId, AccountType.MERCHANT_RECEivable, EntryType.DEBIT, netAmount, currency, paymentId, paymentId);
            createEntry(journal, merchantId + "_SETTLEMENT", AccountType.SETTLEMENT_HOLD, EntryType.CREDIT, netAmount, currency, paymentId, paymentId);
            
            if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
                createEntry(journal, "PLATFORM", AccountType.PLATFORM_FEE_RECEIVABLE, EntryType.CREDIT, platformFee, currency, paymentId, paymentId);
            }
            
            if (gatewayFee.compareTo(BigDecimal.ZERO) > 0) {
                createEntry(journal, "GATEWAY", AccountType.PAYMENT_GATEWAY, EntryType.CREDIT, gatewayFee, currency, paymentId, paymentId);
            }

            journal.setStatus(JournalStatus.POSTED);
            journal.setPostedAt(Instant.now());
        }

        journal = journalRepository.save(journal);
        log.info("Created journal {} for payment {} - {}", journal.getJournalId(), paymentId, journal.getStatus());
        return journal;
    }

    @Transactional
    public Journal createRefundJournal(Payment payment, BigDecimal refundAmount) {
        String paymentId = payment.getId().toString();
        String currency = payment.getCurrency();
        String merchantId = payment.getMerchantId();

        Journal journal = Journal.builder()
                .referenceId(paymentId)
                .referenceType("REFUND")
                .description("Refund for payment: " + paymentId)
                .totalDebit(refundAmount)
                .totalCredit(refundAmount)
                .status(JournalStatus.PENDING)
                .build();
        journal = journalRepository.save(journal);

        createEntry(journal, merchantId, AccountType.MERCHANT_RECEivable, EntryType.DEBIT, refundAmount, currency, paymentId, "REFUND");
        createEntry(journal, merchantId + "_SETTLEMENT", AccountType.SETTLEMENT_HOLD, EntryType.CREDIT, refundAmount, currency, paymentId, "REFUND");
        
        createEntry(journal, "SYSTEM", AccountType.CUSTOMER_ESCROW, EntryType.CREDIT, refundAmount, currency, paymentId, "REFUND");

        journal.setStatus(JournalStatus.POSTED);
        journal.setPostedAt(Instant.now());
        journal = journalRepository.save(journal);
        
        log.info("Created refund journal {} for payment {}", journal.getJournalId(), paymentId);
        return journal;
    }

    private LedgerEntry createEntry(Journal journal, String accountId, AccountType accountType, EntryType entryType, 
                              BigDecimal amount, String currency, String referenceId, String referenceType) {
        BigDecimal balanceBefore = ledgerRepository.getLatestBalance(accountId, accountType);
        if (balanceBefore == null) balanceBefore = BigDecimal.ZERO;
        
        BigDecimal balanceAfter = entryType == EntryType.CREDIT 
                ? balanceBefore.add(amount) 
                : balanceBefore.subtract(amount);

        LedgerEntry entry = LedgerEntry.builder()
                .accountId(accountId)
                .accountType(accountType)
                .entryType(entryType)
                .amount(amount)
                .currency(currency)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .balanceAfter(balanceAfter)
                .journalId(journal.getId())
                .postedAt(Instant.now())
                .build();
        
        entry = ledgerRepository.save(entry);
        
        BigDecimal balanceCheck = ledgerRepository.getLatestBalance(accountId, accountType);
        if (balanceCheck == null) {
            balanceCheck = BigDecimal.ZERO;
        }
        
        return entry;
    }

    public BigDecimal getMerchantBalance(String merchantId) {
        BigDecimal balance = ledgerRepository.getMerchantBalance(merchantId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    public List<LedgerEntry> getMerchantLedger(String merchantId) {
        return ledgerRepository.findByMerchantId(merchantId);
    }

    public List<Journal> getPostedJournals(Instant from, Instant to) {
        return journalRepository.findPostedJournalsBetween(from, to);
    }

    public boolean validateLedgerBalance() {
        BigDecimal totalDebits = ledgerRepository.findAll().stream()
                .filter(e -> e.getPostedAt() != null && e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = ledgerRepository.findAll().stream()
                .filter(e -> e.getPostedAt() != null && e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        boolean balanced = totalDebits.compareTo(totalCredits) == 0;
        log.info("Ledger balance check: debits={}, credits={}, balanced={}", totalDebits, totalCredits, balanced);
        return balanced;
    }
}