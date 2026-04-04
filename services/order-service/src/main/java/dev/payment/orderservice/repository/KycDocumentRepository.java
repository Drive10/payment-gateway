package dev.payment.orderservice.repository;

import dev.payment.orderservice.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByMerchantId(UUID merchantId);
    
    List<KycDocument> findByMerchantIdAndStatus(UUID merchantId, String status);
    
    List<KycDocument> findByStatus(String status);
}
