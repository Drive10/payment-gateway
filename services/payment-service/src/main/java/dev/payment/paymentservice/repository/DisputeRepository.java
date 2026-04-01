package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Dispute;
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
    
    Page<Dispute> findByMerchantId(UUID merchantId, Pageable pageable);
    
    List<Dispute> findByMerchantIdAndStatus(UUID merchantId, Dispute.DisputeStatus status);
    
    List<Dispute> findByPaymentId(UUID paymentId);
    
    boolean existsByPaymentId(UUID paymentId);

    long countByMerchantIdAndStatus(UUID merchantId, Dispute.DisputeStatus status);
}
