package dev.payment.notificationservice.repository;

import dev.payment.notificationservice.domain.DeliveryStatus;
import dev.payment.notificationservice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByStatus(DeliveryStatus status);
}
