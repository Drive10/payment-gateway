package dev.payment.settlementservice.service;

import dev.payment.settlementservice.entity.MerchantSettlement;
import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.entity.SettlementTransaction;
import dev.payment.settlementservice.repository.MerchantSettlementRepository;
import dev.payment.settlementservice.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private MerchantSettlementRepository merchantSettlementRepository;

    @InjectMocks
    private SettlementService settlementService;

    private UUID merchantId;
    private UUID settlementId;
    private String merchantName;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        settlementId = UUID.randomUUID();
        merchantName = "Test Merchant";
        periodStart = LocalDateTime.now().minusDays(1);
        periodEnd = LocalDateTime.now();
    }

    private Settlement createTestSettlement() {
        Settlement settlement = new Settlement();
        settlement.setId(settlementId);
        settlement.setMerchantId(merchantId);
        settlement.setMerchantName(merchantName);
        settlement.setSettlementReference("STL-ABC12345");
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        settlement.setTotalTransactions(0);
        settlement.setTotalAmount(BigDecimal.ZERO);
        settlement.setTotalFees(BigDecimal.ZERO);
        settlement.setTotalRefunds(BigDecimal.ZERO);
        settlement.setNetAmount(BigDecimal.ZERO);
        settlement.setCurrency("INR");
        return settlement;
    }

    private SettlementTransaction createTestTransaction() {
        SettlementTransaction transaction = new SettlementTransaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setTransactionReference("TXN-001");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setFee(new BigDecimal("2.50"));
        transaction.setCurrency("INR");
        transaction.setTransactionType("PAYMENT");
        transaction.setStatus(SettlementTransaction.TransactionStatus.INCLUDED);
        transaction.setTransactionDate(LocalDateTime.now());
        return transaction;
    }

    private MerchantSettlement createTestMerchantSettlement() {
        MerchantSettlement ms = new MerchantSettlement();
        ms.setId(UUID.randomUUID());
        ms.setMerchantId(merchantId);
        ms.setMerchantName(merchantName);
        ms.setCurrentBalance(new BigDecimal("5000.00"));
        ms.setPendingBalance(BigDecimal.ZERO);
        ms.setTotalSettled(new BigDecimal("10000.00"));
        ms.setBankAccountNumber("123456789012");
        ms.setBankIfsc("HDFC0001234");
        ms.setBankName("HDFC Bank");
        ms.setAutoSettle(true);
        return ms;
    }

    @Test
    void createSettlement_shouldCreateAndReturnSettlement() {
        Settlement settlement = createTestSettlement();
        when(settlementRepository.save(any(Settlement.class))).thenReturn(settlement);

        Settlement result = settlementService.createSettlement(merchantId, merchantName, periodStart, periodEnd);

        assertNotNull(result);
        assertEquals(merchantId, result.getMerchantId());
        assertEquals(merchantName, result.getMerchantName());
        assertEquals(Settlement.SettlementStatus.PENDING, result.getStatus());
        assertNotNull(result.getSettlementReference());
        assertTrue(result.getSettlementReference().startsWith("STL-"));
        verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    void createSettlement_shouldGenerateUniqueReference() {
        Settlement settlement1 = createTestSettlement();
        Settlement settlement2 = createTestSettlement();
        settlement2.setId(UUID.randomUUID());
        when(settlementRepository.save(any(Settlement.class)))
                .thenReturn(settlement1)
                .thenReturn(settlement2);

        Settlement result1 = settlementService.createSettlement(merchantId, merchantName, periodStart, periodEnd);
        Settlement result2 = settlementService.createSettlement(merchantId, merchantName, periodStart, periodEnd);

        assertNotNull(result1.getSettlementReference());
        assertNotNull(result2.getSettlementReference());
        verify(settlementRepository, times(2)).save(any(Settlement.class));
    }

    @Test
    void addTransactionToSettlement_shouldUpdateTotals() {
        Settlement settlement = createTestSettlement();
        SettlementTransaction transaction = createTestTransaction();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Settlement result = settlementService.addTransactionToSettlement(settlementId, transaction);

        assertEquals(1, result.getTotalTransactions());
        assertEquals(new BigDecimal("100.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("2.50"), result.getTotalFees());
        assertEquals(new BigDecimal("97.50"), result.getNetAmount());
        verify(settlementRepository).findById(settlementId);
        verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    void addTransactionToSettlement_shouldAccumulateMultipleTransactions() {
        Settlement settlement = createTestSettlement();
        settlement.setTotalTransactions(2);
        settlement.setTotalAmount(new BigDecimal("200.00"));
        settlement.setTotalFees(new BigDecimal("5.00"));
        settlement.setTotalRefunds(new BigDecimal("10.00"));
        settlement.setNetAmount(new BigDecimal("185.00"));

        SettlementTransaction transaction = createTestTransaction();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Settlement result = settlementService.addTransactionToSettlement(settlementId, transaction);

        assertEquals(3, result.getTotalTransactions());
        assertEquals(new BigDecimal("300.00"), result.getTotalAmount());
        assertEquals(new BigDecimal("7.50"), result.getTotalFees());
        verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    void addTransactionToSettlement_shouldThrowWhenSettlementNotFound() {
        SettlementTransaction transaction = createTestTransaction();
        UUID nonExistentId = UUID.randomUUID();

        when(settlementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                settlementService.addTransactionToSettlement(nonExistentId, transaction));

        verify(settlementRepository).findById(nonExistentId);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void processSettlement_shouldTransitionToCompleted() {
        Settlement settlement = createTestSettlement();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Settlement result = settlementService.processSettlement(settlementId);

        assertEquals(Settlement.SettlementStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getPayoutReference());
        assertTrue(result.getPayoutReference().startsWith("PAY-"));
        assertNotNull(result.getPayoutDate());
        assertNotNull(result.getProcessedAt());
        verify(settlementRepository, times(2)).save(any(Settlement.class));
    }

    @Test
    void processSettlement_shouldThrowWhenSettlementNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        when(settlementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                settlementService.processSettlement(nonExistentId));

        verify(settlementRepository).findById(nonExistentId);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void getSettlement_shouldReturnSettlementWhenExists() {
        Settlement settlement = createTestSettlement();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

        Optional<Settlement> result = settlementService.getSettlement(settlementId);

        assertTrue(result.isPresent());
        assertEquals(settlementId, result.get().getId());
        assertEquals(merchantName, result.get().getMerchantName());
        verify(settlementRepository).findById(settlementId);
    }

    @Test
    void getSettlement_shouldReturnEmptyWhenNotFound() {
        when(settlementRepository.findById(settlementId)).thenReturn(Optional.empty());

        Optional<Settlement> result = settlementService.getSettlement(settlementId);

        assertFalse(result.isPresent());
        verify(settlementRepository).findById(settlementId);
    }

    @Test
    void getSettlementByReference_shouldReturnSettlementWhenExists() {
        Settlement settlement = createTestSettlement();
        String reference = "STL-ABC12345";

        when(settlementRepository.findBySettlementReference(reference)).thenReturn(Optional.of(settlement));

        Optional<Settlement> result = settlementService.getSettlementByReference(reference);

        assertTrue(result.isPresent());
        assertEquals(reference, result.get().getSettlementReference());
        verify(settlementRepository).findBySettlementReference(reference);
    }

    @Test
    void getSettlementByReference_shouldReturnEmptyWhenNotFound() {
        String reference = "STL-NONEXISTENT";

        when(settlementRepository.findBySettlementReference(reference)).thenReturn(Optional.empty());

        Optional<Settlement> result = settlementService.getSettlementByReference(reference);

        assertFalse(result.isPresent());
        verify(settlementRepository).findBySettlementReference(reference);
    }

    @Test
    void getMerchantSettlements_shouldReturnListForMerchant() {
        Settlement s1 = createTestSettlement();
        Settlement s2 = createTestSettlement();
        s2.setId(UUID.randomUUID());
        s2.setSettlementReference("STL-XYZ98765");

        Page<Settlement> page = new PageImpl<>(Arrays.asList(s1, s2));
        when(settlementRepository.findByMerchantId(eq(merchantId), any(PageRequest.class))).thenReturn(page);

        List<Settlement> result = settlementService.getMerchantSettlements(merchantId);

        assertEquals(2, result.size());
        assertEquals("STL-ABC12345", result.get(0).getSettlementReference());
        assertEquals("STL-XYZ98765", result.get(1).getSettlementReference());
        verify(settlementRepository).findByMerchantId(eq(merchantId), any(PageRequest.class));
    }

    @Test
    void getMerchantSettlements_shouldReturnEmptyListWhenNoSettlements() {
        Page<Settlement> emptyPage = new PageImpl<>(List.of());
        when(settlementRepository.findByMerchantId(eq(merchantId), any(PageRequest.class))).thenReturn(emptyPage);

        List<Settlement> result = settlementService.getMerchantSettlements(merchantId);

        assertTrue(result.isEmpty());
        verify(settlementRepository).findByMerchantId(eq(merchantId), any(PageRequest.class));
    }

    @Test
    void getPendingSettlements_shouldReturnPendingSettlements() {
        Settlement s1 = createTestSettlement();
        Settlement s2 = createTestSettlement();
        s2.setId(UUID.randomUUID());

        when(settlementRepository.findByStatus(Settlement.SettlementStatus.PENDING))
                .thenReturn(Arrays.asList(s1, s2));

        List<Settlement> result = settlementService.getPendingSettlements();

        assertEquals(2, result.size());
        assertEquals(Settlement.SettlementStatus.PENDING, result.get(0).getStatus());
        assertEquals(Settlement.SettlementStatus.PENDING, result.get(1).getStatus());
        verify(settlementRepository).findByStatus(Settlement.SettlementStatus.PENDING);
    }

    @Test
    void getPendingSettlements_shouldReturnEmptyListWhenNonePending() {
        when(settlementRepository.findByStatus(Settlement.SettlementStatus.PENDING)).thenReturn(List.of());

        List<Settlement> result = settlementService.getPendingSettlements();

        assertTrue(result.isEmpty());
        verify(settlementRepository).findByStatus(Settlement.SettlementStatus.PENDING);
    }

    @Test
    void getOrCreateMerchantSettlement_shouldReturnExistingWhenFound() {
        MerchantSettlement existing = createTestMerchantSettlement();

        when(merchantSettlementRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(existing));

        MerchantSettlement result = settlementService.getOrCreateMerchantSettlement(merchantId, merchantName);

        assertEquals(merchantId, result.getMerchantId());
        assertEquals(merchantName, result.getMerchantName());
        assertEquals(new BigDecimal("5000.00"), result.getCurrentBalance());
        verify(merchantSettlementRepository).findByMerchantId(merchantId);
        verify(merchantSettlementRepository, never()).save(any());
    }

    @Test
    void getOrCreateMerchantSettlement_shouldCreateWhenNotFound() {
        when(merchantSettlementRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> {
            MerchantSettlement ms = invocation.getArgument(0);
            ms.setId(UUID.randomUUID());
            return ms;
        });

        MerchantSettlement result = settlementService.getOrCreateMerchantSettlement(merchantId, merchantName);

        assertEquals(merchantId, result.getMerchantId());
        assertEquals(merchantName, result.getMerchantName());
        assertEquals(BigDecimal.ZERO, result.getCurrentBalance());
        verify(merchantSettlementRepository).findByMerchantId(merchantId);
        verify(merchantSettlementRepository).save(any(MerchantSettlement.class));
    }

    @Test
    void updateMerchantBalance_shouldIncreaseBalance() {
        MerchantSettlement ms = createTestMerchantSettlement();
        ms.setCurrentBalance(new BigDecimal("1000.00"));
        BigDecimal amountToAdd = new BigDecimal("500.00");

        when(merchantSettlementRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(ms));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        settlementService.updateMerchantBalance(merchantId, amountToAdd);

        assertEquals(new BigDecimal("1500.00"), ms.getCurrentBalance());
        verify(merchantSettlementRepository).save(ms);
    }

    @Test
    void updateMerchantBalance_shouldCreateMerchantIfNotExists() {
        BigDecimal amount = new BigDecimal("750.00");

        when(merchantSettlementRepository.findByMerchantId(merchantId)).thenReturn(Optional.empty());
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> {
            MerchantSettlement ms = invocation.getArgument(0);
            ms.setId(UUID.randomUUID());
            return ms;
        });

        settlementService.updateMerchantBalance(merchantId, amount);

        verify(merchantSettlementRepository, times(2)).save(any(MerchantSettlement.class));
    }

    @Test
    void updateMerchantBalance_shouldHandleNegativeAmount() {
        MerchantSettlement ms = createTestMerchantSettlement();
        ms.setCurrentBalance(new BigDecimal("1000.00"));
        BigDecimal negativeAmount = new BigDecimal("-200.00");

        when(merchantSettlementRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(ms));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        settlementService.updateMerchantBalance(merchantId, negativeAmount);

        assertEquals(new BigDecimal("800.00"), ms.getCurrentBalance());
        verify(merchantSettlementRepository).save(ms);
    }

    @Test
    void getAllMerchantSettlements_shouldReturnAllMerchants() {
        MerchantSettlement ms1 = createTestMerchantSettlement();
        MerchantSettlement ms2 = createTestMerchantSettlement();
        ms2.setId(UUID.randomUUID());
        ms2.setMerchantId(UUID.randomUUID());
        ms2.setMerchantName("Another Merchant");

        when(merchantSettlementRepository.findAll()).thenReturn(Arrays.asList(ms1, ms2));

        List<MerchantSettlement> result = settlementService.getAllMerchantSettlements();

        assertEquals(2, result.size());
        assertEquals("Test Merchant", result.get(0).getMerchantName());
        assertEquals("Another Merchant", result.get(1).getMerchantName());
        verify(merchantSettlementRepository).findAll();
    }

    @Test
    void getAllMerchantSettlements_shouldReturnEmptyListWhenNoneExist() {
        when(merchantSettlementRepository.findAll()).thenReturn(List.of());

        List<MerchantSettlement> result = settlementService.getAllMerchantSettlements();

        assertTrue(result.isEmpty());
        verify(merchantSettlementRepository).findAll();
    }
}
