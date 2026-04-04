package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    
    Optional<Settlement> findBySettlementReference(String settlementReference);
    
    Page<Settlement> findByMerchantId(UUID merchantId, Pageable pageable);
    
    List<Settlement> findByStatus(Settlement.SettlementStatus status);
    
    @Query("SELECT s FROM Settlement s WHERE s.status = :status AND s.createdAt < :before")
    List<Settlement> findByStatusAndCreatedAtBefore(
        @Param("status") Settlement.SettlementStatus status,
        @Param("before") LocalDateTime before
    );
    
    @Query("SELECT s FROM Settlement s WHERE s.merchantId = :merchantId AND s.periodStart >= :start AND s.periodEnd <= :end")
    List<Settlement> findByMerchantIdAndPeriod(
        @Param("merchantId") UUID merchantId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT COUNT(s) FROM Settlement s WHERE s.status = :status")
    long countByStatus(@Param("status") Settlement.SettlementStatus status);
}
