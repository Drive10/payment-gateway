package dev.payment.paymentservice.order.repository;

import dev.payment.paymentservice.order.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByEmail(String email);
    
    Optional<Merchant> findByApiKey(String apiKey);
    
    List<Merchant> findByStatus(String status);
    
    List<Merchant> findByKycStatus(String kycStatus);
    
    boolean existsByEmail(String email);
}
