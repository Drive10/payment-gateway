package dev.payment.paymentservice.service;

import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.LedgerEntry.AccountType;
import dev.payment.paymentservice.entity.LedgerEntry.EntryType;
import dev.payment.paymentservice.entity.SettlementBatch.SettlementStatus;
import dev.payment.paymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementServiceTest {

    private SettlementBatchRepository settlementRepository;
    private LedgerEntryRepository ledgerRepository;
    private PaymentRepository paymentRepository;
    private LedgerService ledgerService;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementRepository = mock(SettlementBatchRepository.class);
        ledgerRepository = mock(LedgerEntryRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        ledgerService = mock(LedgerService.class);

        settlementService = new SettlementService(settlementRepository, ledgerRepository, paymentRepository, ledgerService);
    }

    @Test
    void createSettlementBatch_ShouldCreatePendingBatch() {
        String merchantId = "merchant-123";
        String currency = "INR";

        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId(merchantId)
                .accountType(AccountType.SETTLEMENT_HOLD)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("50000"))
                .currency("INR")
                .merchantId(merchantId)
                .postedAt(Instant.now())
                .build();

        when(ledgerService.getMerchantBalance(merchantId)).thenReturn(new BigDecimal("50000"));
        when(ledgerRepository.findByMerchantId(merchantId)).thenReturn(List.of(entry));
        when(settlementRepository.save(any(SettlementBatch.class))).thenAnswer(invocation -> {
            SettlementBatch batch = invocation.getArgument(0);
            batch.setBatchId(UUID.randomUUID().toString());
            return batch;
        });

        SettlementBatch batch = settlementService.createSettlementBatch(merchantId, currency);

        assertNotNull(batch);
        assertEquals(merchantId, batch.getMerchantId());
        assertEquals(SettlementStatus.PENDING, batch.getStatus());
        assertEquals(new BigDecimal("50000"), batch.getTotalAmount());
        assertEquals(new BigDecimal("500").setScale(4), batch.getFeeAmount());
        verify(settlementRepository, times(1)).save(any(SettlementBatch.class));
    }

    @Test
    void createSettlementBatch_WithNoBalance_ShouldThrowException() {
        when(ledgerService.getMerchantBalance("merchant-123")).thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalStateException.class,
                () -> settlementService.createSettlementBatch("merchant-123", "INR"));
    }

    @Test
    void approveBatch_ShouldTransitionToApproved() {
        UUID batchPk = UUID.randomUUID();
        SettlementBatch batch = SettlementBatch.builder()
                .batchId("batch-001")
                .merchantId("merchant-123")
                .totalAmount(new BigDecimal("50000"))
                .feeAmount(new BigDecimal("500"))
                .netAmount(new BigDecimal("49500"))
                .currency("INR")
                .status(SettlementStatus.PENDING)
                .build();
        batch.setId(batchPk);

        when(settlementRepository.findById(batchPk)).thenReturn(Optional.of(batch));
        when(settlementRepository.save(any(SettlementBatch.class))).thenReturn(batch);

        SettlementBatch result = settlementService.approveBatch(batchPk);

        assertEquals(SettlementStatus.APPROVED, result.getStatus());
    }

    @Test
    void approveBatch_WithNonPendingBatch_ShouldThrowException() {
        UUID batchPk = UUID.randomUUID();
        SettlementBatch batch = SettlementBatch.builder()
                .batchId("batch-002")
                .status(SettlementStatus.COMPLETED)
                .build();
        batch.setId(batchPk);

        when(settlementRepository.findById(batchPk)).thenReturn(Optional.of(batch));

        assertThrows(IllegalStateException.class, () -> settlementService.approveBatch(batchPk));
    }

    @Test
    void initiatePayout_ShouldTransitionToProcessing() {
        UUID batchPk = UUID.randomUUID();
        SettlementBatch batch = SettlementBatch.builder()
                .batchId("batch-003")
                .merchantId("merchant-123")
                .totalAmount(new BigDecimal("50000"))
                .feeAmount(new BigDecimal("500"))
                .netAmount(new BigDecimal("49500"))
                .currency("INR")
                .status(SettlementStatus.APPROVED)
                .build();
        batch.setId(batchPk);

        when(settlementRepository.findById(batchPk)).thenReturn(Optional.of(batch));
        when(settlementRepository.save(any(SettlementBatch.class))).thenReturn(batch);

        SettlementBatch result = settlementService.initiatePayout(batchPk, "BANK_TRANSFER");

        assertEquals(SettlementStatus.PROCESSING, result.getStatus());
        assertEquals("BANK_TRANSFER", result.getPayoutMethod());
        assertNotNull(result.getPayoutReference());
    }

    @Test
    void processSettlementBatches_ShouldProcessApprovedBatches() {
        SettlementBatch approved = SettlementBatch.builder()
                .batchId("batch-004")
                .merchantId("merchant-123")
                .totalAmount(new BigDecimal("50000"))
                .feeAmount(new BigDecimal("500"))
                .netAmount(new BigDecimal("49500"))
                .currency("INR")
                .status(SettlementStatus.APPROVED)
                .build();
        approved.setId(UUID.randomUUID());

        LedgerEntry entry = LedgerEntry.builder()
                .id(UUID.randomUUID())
                .accountId("merchant-123")
                .accountType(AccountType.SETTLEMENT_HOLD)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("50000"))
                .currency("INR")
                .merchantId("merchant-123")
                .postedAt(Instant.now())
                .reference("ref-1")
                .paymentId("payment-1")
                .build();

        when(settlementRepository.findByStatus(SettlementStatus.APPROVED)).thenReturn(List.of(approved));
        when(ledgerRepository.findByMerchantId("merchant-123")).thenReturn(List.of(entry));
        when(settlementRepository.save(any(SettlementBatch.class))).thenReturn(approved);

        settlementService.processSettlementBatches();

        verify(settlementRepository, times(1)).save(any(SettlementBatch.class));
        verify(ledgerRepository, atLeastOnce()).save(any(LedgerEntry.class));
    }

    @Test
    void getMerchantSettlements_ShouldReturnSettlements() {
        String merchantId = "merchant-123";
        SettlementBatch batch1 = SettlementBatch.builder()
                .batchId("batch-005").merchantId(merchantId)
                .status(SettlementStatus.COMPLETED).build();
        SettlementBatch batch2 = SettlementBatch.builder()
                .batchId("batch-006").merchantId(merchantId)
                .status(SettlementStatus.PENDING).build();

        when(settlementRepository.findByMerchantId(merchantId)).thenReturn(List.of(batch1, batch2));

        List<SettlementBatch> result = settlementService.getMerchantSettlements(merchantId);
        assertEquals(2, result.size());
    }

    @Test
    void getMerchantPendingSettlement_ShouldReturnAmount() {
        String merchantId = "merchant-123";
        when(settlementRepository.sumPendingSettlementByMerchant(merchantId)).thenReturn(new BigDecimal("25000"));

        BigDecimal result = settlementService.getMerchantPendingSettlement(merchantId);
        assertEquals(new BigDecimal("25000"), result);
    }

    @Test
    void getMerchantPendingSettlement_WithNullResult_ShouldReturnZero() {
        when(settlementRepository.sumPendingSettlementByMerchant("merchant-123")).thenReturn(null);

        BigDecimal result = settlementService.getMerchantPendingSettlement("merchant-123");
        assertEquals(BigDecimal.ZERO, result);
    }
}
