package dev.payment.notificationservice.repository;

import dev.payment.notificationservice.domain.ConsumedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEvent, UUID> {
    boolean existsByEventId(String eventId);
}
