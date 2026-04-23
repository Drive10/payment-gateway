package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
    
    Optional<Dispute> findByDisputeReference(String reference);
    
    boolean existsByDisputeReference(String reference);
    
    Page<Dispute> findByMerchantId(UUID merchantId, Pageable pageable);
    
    Page<Dispute> findByMerchantIdAndStatus(UUID merchantId, Dispute.DisputeStatus status, Pageable pageable);
    
    Page<Dispute> findByStatus(Dispute.DisputeStatus status, Pageable pageable);
    
    List<Dispute> findByPaymentId(UUID paymentId);
    
    boolean existsByPaymentId(UUID paymentId);

    long countByMerchantIdAndStatus(UUID merchantId, Dispute.DisputeStatus status);
}
