package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByDisputeId(String disputeId);
    
    Optional<Dispute> findByPaymentId(String paymentId);
    
    List<Dispute> findByMerchantId(UUID merchantId);
    
    List<Dispute> findByMerchantIdAndStatus(UUID merchantId, String status);
    
    List<Dispute> findByStatus(String status);
    
    List<Dispute> findByPriority(String priority);
}
