package dev.payment.notificationservice.infrastructure.persistence.repository;

import dev.payment.notificationservice.domain.entities.Notification;
import dev.payment.notificationservice.domain.entities.NotificationStatus;
import dev.payment.notificationservice.domain.entities.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status);
}
