package dev.payment.analyticsservice.service;

import dev.payment.analyticsservice.entity.MerchantSettlement;
import dev.payment.analyticsservice.entity.Settlement;
import dev.payment.analyticsservice.repository.MerchantSettlementRepository;
import dev.payment.analyticsservice.repository.SettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduler.class);

    private final SettlementRepository settlementRepository;
    private final MerchantSettlementRepository merchantSettlementRepository;
    private final SettlementService settlementService;

    @Value("${application.settlement.enabled:true}")
    private boolean settlementEnabled;

    @Value("${application.settlement.minimum-amount:1000}")
    private BigDecimal minimumSettlement;

    public SettlementScheduler(
            SettlementRepository settlementRepository,
            MerchantSettlementRepository merchantSettlementRepository,
            SettlementService settlementService
    ) {
        this.settlementRepository = settlementRepository;
        this.merchantSettlementRepository = merchantSettlementRepository;
        this.settlementService = settlementService;
    }

    @Scheduled(cron = "${application.settlement.cron:0 0 6 * * ?}")
    @Transactional
    public void processScheduledSettlements() {
        if (!settlementEnabled) {
            log.info("Scheduled settlements disabled");
            return;
        }

        log.info("Starting scheduled settlement processing");
        List<MerchantSettlement> eligibleMerchants = merchantSettlementRepository
                .findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(minimumSettlement);

        log.info("Found {} eligible merchants for settlement", eligibleMerchants.size());

        LocalDateTime periodEnd = LocalDateTime.now();
        LocalDateTime periodStart = periodEnd.minusDays(1);

        for (MerchantSettlement merchant : eligibleMerchants) {
            try {
                processMerchantSettlement(merchant, periodStart, periodEnd);
            } catch (Exception e) {
                log.error("Settlement processing failed for merchant {}: {}", merchant.getMerchantId(), e.getMessage(), e);
                log.error("Failed to process settlement for merchant {}: {}", 
                        merchant.getMerchantId(), e.getMessage());
            }
        }

        log.info("Completed scheduled settlement processing");
    }

    private void processMerchantSettlement(MerchantSettlement merchant, LocalDateTime periodStart, LocalDateTime periodEnd) {
        BigDecimal availableBalance = merchant.getCurrentBalance();

        if (availableBalance.compareTo(minimumSettlement) < 0) {
            log.debug("Merchant {} balance {} below minimum {}", 
                    merchant.getMerchantId(), availableBalance, minimumSettlement);
            return;
        }

        Settlement settlement = settlementService.createSettlement(
                merchant.getMerchantId(),
                merchant.getMerchantName(),
                periodStart,
                periodEnd
        );

        settlement.setTotalAmount(availableBalance);
        settlement.setTotalFees(BigDecimal.ZERO);
        settlement.setTotalRefunds(BigDecimal.ZERO);
        settlement.setNetAmount(availableBalance);
        settlement.setBankAccountNumber(maskAccount(merchant.getBankAccountNumber()));
        settlement.setBankIfsc(merchant.getBankIfsc());
        settlement.setBankName(merchant.getBankName());

        settlementRepository.save(settlement);

        settlementService.processSettlement(settlement.getId());

        merchant.setCurrentBalance(BigDecimal.ZERO);
        merchant.setPendingBalance(merchant.getPendingBalance().add(availableBalance));
        merchant.setTotalSettled(merchant.getTotalSettled().add(availableBalance));
        merchantSettlementRepository.save(merchant);

        log.info("Processed settlement {} for merchant {} amount {}", 
                settlement.getSettlementReference(), merchant.getMerchantId(), availableBalance);
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
