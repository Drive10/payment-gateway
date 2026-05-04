package dev.payment.paymentservice.service;

import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.SettlementBatch.SettlementStatus;
import dev.payment.paymentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final SettlementBatchRepository settlementRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void processSettlementBatches() {
        List<SettlementBatch> pendingBatches = settlementRepository.findByStatus(SettlementStatus.APPROVED);
        
        for (SettlementBatch batch : pendingBatches) {
            try {
                processBatch(batch);
            } catch (Exception e) {
                log.error("Failed to process batch {}: {}", batch.getBatchId(), e.getMessage());
                batch.setStatus(SettlementStatus.FAILED);
                batch.setFailureReason(e.getMessage());
                settlementRepository.save(batch);
            }
        }
    }

    @Transactional
    public SettlementBatch createSettlementBatch(String merchantId, String currency) {
        BigDecimal pendingBalance = ledgerService.getMerchantBalance(merchantId);
        
        if (pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No pending balance for merchant: " + merchantId);
        }

        List<LedgerEntry> entries = ledgerRepository.findByMerchantId(merchantId).stream()
                .filter(e -> e.getPostedAt() != null)
                .filter(e -> "SETTLEMENT_HOLD".equals(e.getAccountType().name()))
                .filter(e -> e.getEntryType() == LedgerEntry.EntryType.CREDIT)
                .toList();

        BigDecimal settleAmount = entries.stream()
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fee = calculateSettlementFee(settleAmount);
        BigDecimal netAmount = settleAmount.subtract(fee);

        SettlementBatch batch = SettlementBatch.builder()
                .merchantId(merchantId)
                .totalAmount(settleAmount)
                .feeAmount(fee)
                .netAmount(netAmount)
                .currency(currency)
                .status(SettlementStatus.PENDING)
                .build();
        
        batch = settlementRepository.save(batch);
        
        log.info("Created settlement batch {} for merchant {}: amount={}", 
                batch.getBatchId(), merchantId, settleAmount);
        
        return batch;
    }

    @Transactional
    public SettlementBatch approveBatch(UUID batchId) {
        SettlementBatch batch = settlementRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        
        if (!batch.canApprove()) {
            throw new IllegalStateException("Batch cannot be approved");
        }
        
        batch.setStatus(SettlementStatus.APPROVED);
        batch = settlementRepository.save(batch);
        
        log.info("Approved settlement batch {}", batch.getBatchId());
        return batch;
    }

    @Transactional
    public SettlementBatch initiatePayout(UUID batchId, String payoutMethod) {
        SettlementBatch batch = settlementRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
        
        if (!batch.canProcess()) {
            throw new IllegalStateException("Batch cannot be processed");
        }
        
        batch.setStatus(SettlementStatus.PROCESSING);
        batch.setPayoutMethod(payoutMethod);
        batch.setPayoutReference("PAYOUT_" + System.currentTimeMillis());
        batch = settlementRepository.save(batch);
        
        log.info("Initiated payout for batch {}: method={}, reference={}", 
                batch.getBatchId(), payoutMethod, batch.getPayoutReference());
        
        return batch;
    }

    private void processBatch(SettlementBatch batch) {
        batch.setStatus(SettlementStatus.COMPLETED);
        batch.setSettledAt(Instant.now());
        
        String merchantId = batch.getMerchantId();
        String paymentRef = "SETTLEMENT_" + batch.getBatchId();
        
        for (LedgerEntry entry : ledgerRepository.findByMerchantId(merchantId)) {
            if ("SETTLEMENT_HOLD".equals(entry.getAccountType().name()) && 
                entry.getEntryType() == LedgerEntry.EntryType.CREDIT &&
                entry.getPostedAt() != null) {
                
                entry.setEntryType(LedgerEntry.EntryType.DEBIT);
                ledgerRepository.save(entry);
            }
        }
        
        settlementRepository.save(batch);
        
        log.info("Completed settlement batch {} for merchant {}: netAmount={}", 
                batch.getBatchId(), merchantId, batch.getNetAmount());
    }

    private BigDecimal calculateSettlementFee(BigDecimal amount) {
        return amount.multiply(new BigDecimal("0.01")).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    public List<SettlementBatch> getMerchantSettlements(String merchantId) {
        return settlementRepository.findByMerchantId(merchantId);
    }

    public BigDecimal getMerchantPendingSettlement(String merchantId) {
        BigDecimal amount = settlementRepository.sumPendingSettlementByMerchant(merchantId);
        return amount != null ? amount : BigDecimal.ZERO;
    }

    public BigDecimal getMerchantSettledAmount(String merchantId) {
        BigDecimal amount = settlementRepository.sumSettledAmountByMerchant(merchantId);
        return amount != null ? amount : BigDecimal.ZERO;
    }
}