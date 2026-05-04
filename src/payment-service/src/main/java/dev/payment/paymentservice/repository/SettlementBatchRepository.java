package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.SettlementBatch;
import dev.payment.paymentservice.entity.SettlementBatch.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {
    
    @Query("SELECT sb FROM SettlementBatch sb WHERE sb.merchantId = :merchantId ORDER BY sb.createdAt DESC")
    List<SettlementBatch> findByMerchantId(String merchantId);
    
    @Query("SELECT sb FROM SettlementBatch sb WHERE sb.status = :status ORDER BY sb.createdAt ASC")
    List<SettlementBatch> findByStatus(SettlementStatus status);
    
    @Query("SELECT sb FROM SettlementBatch sb WHERE sb.merchantId = :merchantId AND sb.status IN :statuses")
    List<SettlementBatch> findByMerchantIdAndStatusIn(String merchantId, List<SettlementStatus> statuses);
    
    @Query("SELECT SUM(sb.netAmount) FROM SettlementBatch sb WHERE sb.merchantId = :merchantId AND sb.status = 'COMPLETED'")
    BigDecimal sumSettledAmountByMerchant(String merchantId);
    
    @Query("SELECT SUM(sb.netAmount) FROM SettlementBatch sb WHERE sb.merchantId = :merchantId AND sb.status IN ('PENDING', 'APPROVED', 'PROCESSING')")
    BigDecimal sumPendingSettlementByMerchant(String merchantId);
    
    @Query("SELECT sb FROM SettlementBatch sb WHERE sb.settledAt >= :fromDate AND sb.settledAt < :toDate AND sb.status = 'COMPLETED'")
    List<SettlementBatch> findCompletedSettlementsBetween(java.time.Instant fromDate, java.time.Instant toDate);
}