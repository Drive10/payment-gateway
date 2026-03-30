package dev.payment.authservice.repository;

import dev.payment.authservice.domain.ApiClient;
import dev.payment.authservice.domain.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {
    Page<ApiClient> findByStatus(ClientStatus status, Pageable pageable);
    Optional<ApiClient> findByClientCode(String clientCode);
    Optional<ApiClient> findByApiKey(String apiKey);
    boolean existsByClientCode(String clientCode);
}
