package dev.payment.authservice.repository;

import dev.payment.authservice.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByEmail(@Param("email") String email);
    Optional<Merchant> findByApiKey(@Param("apiKey") String apiKey);
    boolean existsByEmail(@Param("email") String email);
}
