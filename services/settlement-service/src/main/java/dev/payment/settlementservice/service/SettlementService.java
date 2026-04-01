package dev.payment.settlementservice.service;

import dev.payment.settlementservice.entity.MerchantSettlement;
import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.entity.SettlementTransaction;
import dev.payment.settlementservice.repository.MerchantSettlementRepository;
import dev.payment.settlementservice.repository.SettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementService {
    
    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);
    
    private final SettlementRepository settlementRepository;
    private final MerchantSettlementRepository merchantSettlementRepository;
    
    public SettlementService(
            SettlementRepository settlementRepository,
            MerchantSettlementRepository merchantSettlementRepository) {
        this.settlementRepository = settlementRepository;
        this.merchantSettlementRepository = merchantSettlementRepository;
    }
    
    @Transactional
    public Settlement createSettlement(UUID merchantId, String merchantName, 
                                       LocalDateTime periodStart, LocalDateTime periodEnd) {
        Settlement settlement = new Settlement();
        settlement.setMerchantId(merchantId);
        settlement.setMerchantName(merchantName);
        settlement.setPeriodStart(periodStart);
        settlement.setPeriodEnd(periodEnd);
        settlement.setSettlementReference("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        settlement.setStatus(Settlement.SettlementStatus.PENDING);
        
        return settlementRepository.save(settlement);
    }
    
    @Transactional
    public Settlement addTransactionToSettlement(UUID settlementId, SettlementTransaction transaction) {
        Settlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));
        
        settlement.setTotalTransactions(settlement.getTotalTransactions() + 1);
        settlement.setTotalAmount(settlement.getTotalAmount().add(transaction.getAmount()));
        settlement.setTotalFees(settlement.getTotalFees().add(transaction.getFee()));
        settlement.setNetAmount(settlement.getTotalAmount().subtract(settlement.getTotalFees()).subtract(settlement.getTotalRefunds()));
        
        return settlementRepository.save(settlement);
    }
    
    @Transactional
    public Settlement processSettlement(UUID settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
            .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));
        
        settlement.setStatus(Settlement.SettlementStatus.PROCESSING);
        settlement = settlementRepository.save(settlement);
        
        // Simulate payout processing
        settlement.setPayoutReference("PAY-" + System.currentTimeMillis());
        settlement.setPayoutDate(LocalDateTime.now());
        settlement.setProcessedAt(LocalDateTime.now());
        settlement.setStatus(Settlement.SettlementStatus.COMPLETED);
        
        return settlementRepository.save(settlement);
    }
    
    public Optional<Settlement> getSettlement(UUID id) {
        return settlementRepository.findById(id);
    }
    
    public Optional<Settlement> getSettlementByReference(String reference) {
        return settlementRepository.findBySettlementReference(reference);
    }
    
    public List<Settlement> getMerchantSettlements(UUID merchantId) {
        return settlementRepository.findByMerchantId(merchantId, 
            org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
    }
    
    public List<Settlement> getPendingSettlements() {
        return settlementRepository.findByStatus(Settlement.SettlementStatus.PENDING);
    }
    
    public MerchantSettlement getOrCreateMerchantSettlement(UUID merchantId, String merchantName) {
        return merchantSettlementRepository.findByMerchantId(merchantId)
            .orElseGet(() -> {
                MerchantSettlement ms = new MerchantSettlement();
                ms.setMerchantId(merchantId);
                ms.setMerchantName(merchantName);
                return merchantSettlementRepository.save(ms);
            });
    }
    
    @Transactional
    public void updateMerchantBalance(UUID merchantId, BigDecimal amount) {
        MerchantSettlement ms = getOrCreateMerchantSettlement(merchantId, "Merchant");
        ms.setCurrentBalance(ms.getCurrentBalance().add(amount));
        merchantSettlementRepository.save(ms);
    }
    
    public List<MerchantSettlement> getAllMerchantSettlements() {
        return merchantSettlementRepository.findAll();
    }
}
