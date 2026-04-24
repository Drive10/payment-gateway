package dev.payment.notificationservice.infrastructure.persistence.repository;

import dev.payment.notificationservice.domain.entities.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByKey(String key);
}
