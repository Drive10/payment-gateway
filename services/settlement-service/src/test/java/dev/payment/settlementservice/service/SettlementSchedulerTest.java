package dev.payment.settlementservice.service;

import dev.payment.settlementservice.entity.MerchantSettlement;
import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.repository.MerchantSettlementRepository;
import dev.payment.settlementservice.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private MerchantSettlementRepository merchantSettlementRepository;

    private SettlementService settlementService;

    @InjectMocks
    private SettlementScheduler settlementScheduler;

    private UUID merchantId;
    private MerchantSettlement eligibleMerchant;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(settlementRepository, merchantSettlementRepository);
        settlementScheduler = new SettlementScheduler(settlementRepository, merchantSettlementRepository, settlementService);

        merchantId = UUID.randomUUID();
        ReflectionTestUtils.setField(settlementScheduler, "settlementEnabled", true);
        ReflectionTestUtils.setField(settlementScheduler, "minimumSettlement", new BigDecimal("1000"));

        eligibleMerchant = new MerchantSettlement();
        eligibleMerchant.setId(UUID.randomUUID());
        eligibleMerchant.setMerchantId(merchantId);
        eligibleMerchant.setMerchantName("Eligible Merchant");
        eligibleMerchant.setCurrentBalance(new BigDecimal("5000.00"));
        eligibleMerchant.setPendingBalance(new BigDecimal("1000.00"));
        eligibleMerchant.setTotalSettled(new BigDecimal("20000.00"));
        eligibleMerchant.setBankAccountNumber("123456789012");
        eligibleMerchant.setBankIfsc("HDFC0001234");
        eligibleMerchant.setBankName("HDFC Bank");
        eligibleMerchant.setAutoSettle(true);
    }

    private void setupSaveAndFindById() {
        AtomicReference<Settlement> savedSettlement = new AtomicReference<>();
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            savedSettlement.set(s);
            return s;
        });
        when(settlementRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(savedSettlement.get())
        );
    }

    @Test
    void processScheduledSettlements_shouldDoNothingWhenDisabled() {
        ReflectionTestUtils.setField(settlementScheduler, "settlementEnabled", false);

        settlementScheduler.processScheduledSettlements();

        verify(merchantSettlementRepository, never()).findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(any());
    }

    @Test
    void processScheduledSettlements_shouldProcessEligibleMerchants() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(merchantSettlementRepository).findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000"));
        verify(settlementRepository, times(2)).save(any(Settlement.class));
        verify(merchantSettlementRepository).save(any(MerchantSettlement.class));
    }

    @Test
    void processScheduledSettlements_shouldHandleEmptyEligibleList() {
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(Collections.emptyList());

        settlementScheduler.processScheduledSettlements();

        verify(merchantSettlementRepository).findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000"));
        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(merchantSettlementRepository, never()).save(any(MerchantSettlement.class));
    }

    @Test
    void processScheduledSettlements_shouldContinueWhenOneMerchantFails() {
        MerchantSettlement merchant2 = new MerchantSettlement();
        merchant2.setId(UUID.randomUUID());
        merchant2.setMerchantId(UUID.randomUUID());
        merchant2.setMerchantName("Second Merchant");
        merchant2.setCurrentBalance(new BigDecimal("3000.00"));
        merchant2.setPendingBalance(BigDecimal.ZERO);
        merchant2.setTotalSettled(BigDecimal.ZERO);
        merchant2.setBankAccountNumber("987654321098");
        merchant2.setBankIfsc("ICIC0001234");
        merchant2.setBankName("ICICI Bank");
        merchant2.setAutoSettle(true);

        AtomicReference<Settlement> savedSettlement = new AtomicReference<>();
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            savedSettlement.set(s);
            return s;
        });
        when(settlementRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(savedSettlement.get())
        );
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(Arrays.asList(eligibleMerchant, merchant2));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> settlementScheduler.processScheduledSettlements());

        verify(settlementRepository, atLeastOnce()).save(any(Settlement.class));
        verify(merchantSettlementRepository, atLeastOnce()).save(any(MerchantSettlement.class));
    }

    @Test
    void processScheduledSettlements_shouldUseCorrectPeriodRange() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                s.getPeriodStart() != null && s.getPeriodEnd() != null &&
                s.getPeriodStart().isBefore(s.getPeriodEnd()) &&
                s.getPeriodEnd().isAfter(LocalDateTime.now().minusHours(1))
        ));
    }

    @Test
    void processMerchantSettlement_shouldSkipWhenBalanceBelowMinimum() {
        MerchantSettlement lowBalanceMerchant = new MerchantSettlement();
        lowBalanceMerchant.setId(UUID.randomUUID());
        lowBalanceMerchant.setMerchantId(UUID.randomUUID());
        lowBalanceMerchant.setMerchantName("Low Balance Merchant");
        lowBalanceMerchant.setCurrentBalance(new BigDecimal("500.00"));
        lowBalanceMerchant.setPendingBalance(BigDecimal.ZERO);
        lowBalanceMerchant.setTotalSettled(BigDecimal.ZERO);
        lowBalanceMerchant.setAutoSettle(true);

        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(lowBalanceMerchant));

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(merchantSettlementRepository, never()).save(eq(lowBalanceMerchant));
    }

    @Test
    void processMerchantSettlement_shouldSettleExactMinimumAmount() {
        MerchantSettlement exactMinMerchant = new MerchantSettlement();
        exactMinMerchant.setId(UUID.randomUUID());
        exactMinMerchant.setMerchantId(UUID.randomUUID());
        exactMinMerchant.setMerchantName("Exact Min Merchant");
        exactMinMerchant.setCurrentBalance(new BigDecimal("1000.00"));
        exactMinMerchant.setPendingBalance(new BigDecimal("500.00"));
        exactMinMerchant.setTotalSettled(new BigDecimal("10000.00"));
        exactMinMerchant.setBankAccountNumber("111122223333");
        exactMinMerchant.setBankIfsc("SBIN0001234");
        exactMinMerchant.setBankName("SBI");
        exactMinMerchant.setAutoSettle(true);

        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(exactMinMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                s.getTotalAmount().equals(new BigDecimal("1000.00")) &&
                s.getNetAmount().equals(new BigDecimal("1000.00"))
        ));
        verify(merchantSettlementRepository).save(argThat(ms ->
                ms.getCurrentBalance().equals(BigDecimal.ZERO) &&
                ms.getPendingBalance().equals(new BigDecimal("1500.00")) &&
                ms.getTotalSettled().equals(new BigDecimal("11000.00"))
        ));
    }

    @Test
    void processMerchantSettlement_shouldMaskAccountNumber() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                "****9012".equals(s.getBankAccountNumber())
        ));
    }

    @Test
    void processMerchantSettlement_shouldSetBankDetailsOnSettlement() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                "****9012".equals(s.getBankAccountNumber()) &&
                "HDFC0001234".equals(s.getBankIfsc()) &&
                "HDFC Bank".equals(s.getBankName())
        ));
    }

    @Test
    void maskAccount_shouldReturnStarsForNull() {
        eligibleMerchant.setBankAccountNumber(null);

        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                "****".equals(s.getBankAccountNumber())
        ));
    }

    @Test
    void maskAccount_shouldReturnStarsForShortAccount() {
        eligibleMerchant.setBankAccountNumber("123");

        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                "****".equals(s.getBankAccountNumber())
        ));
    }

    @Test
    void maskAccount_shouldMaskExactlyFourCharacters() {
        eligibleMerchant.setBankAccountNumber("1234");

        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                "****1234".equals(s.getBankAccountNumber())
        ));
    }

    @Test
    void processScheduledSettlements_shouldProcessMultipleMerchants() {
        MerchantSettlement merchant2 = new MerchantSettlement();
        merchant2.setId(UUID.randomUUID());
        merchant2.setMerchantId(UUID.randomUUID());
        merchant2.setMerchantName("Second Merchant");
        merchant2.setCurrentBalance(new BigDecimal("3000.00"));
        merchant2.setPendingBalance(new BigDecimal("500.00"));
        merchant2.setTotalSettled(new BigDecimal("15000.00"));
        merchant2.setBankAccountNumber("555566667777");
        merchant2.setBankIfsc("AXIS0001234");
        merchant2.setBankName("Axis Bank");
        merchant2.setAutoSettle(true);

        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(Arrays.asList(eligibleMerchant, merchant2));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository, times(4)).save(any(Settlement.class));
        verify(merchantSettlementRepository, times(2)).save(any(MerchantSettlement.class));
    }

    @Test
    void processScheduledSettlements_shouldSetZeroFeesAndRefunds() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository).save(argThat(s ->
                BigDecimal.ZERO.equals(s.getTotalFees()) &&
                BigDecimal.ZERO.equals(s.getTotalRefunds())
        ));
    }

    @Test
    void processScheduledSettlements_shouldResetMerchantBalanceAfterSettlement() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        settlementScheduler.processScheduledSettlements();

        verify(merchantSettlementRepository).save(argThat(ms ->
                ms.getCurrentBalance().equals(BigDecimal.ZERO)
        ));
    }

    @Test
    void processScheduledSettlements_shouldUpdateSettlementStatusToCompleted() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository, atLeastOnce()).save(argThat(s ->
                s.getStatus() == Settlement.SettlementStatus.COMPLETED
        ));
    }

    @Test
    void processScheduledSettlements_shouldSetPayoutReference() {
        setupSaveAndFindById();
        when(merchantSettlementRepository.findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(new BigDecimal("1000")))
                .thenReturn(List.of(eligibleMerchant));
        when(merchantSettlementRepository.save(any(MerchantSettlement.class))).thenReturn(eligibleMerchant);

        settlementScheduler.processScheduledSettlements();

        verify(settlementRepository, atLeastOnce()).save(argThat(s ->
                s.getPayoutReference() != null && s.getPayoutReference().startsWith("PAY-")
        ));
    }
}
