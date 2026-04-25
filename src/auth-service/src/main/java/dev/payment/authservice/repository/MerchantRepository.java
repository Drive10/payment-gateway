package dev.payment.authservice.repository;

import dev.payment.authservice.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByEmail(String email);
    Optional<Merchant> findByApiKey(String apiKey);
    boolean existsByEmail(String email);
}