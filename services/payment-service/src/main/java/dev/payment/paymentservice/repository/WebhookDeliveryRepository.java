package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.WebhookDelivery;
import dev.payment.paymentservice.domain.enums.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findByStatusAndNextAttemptAtBefore(WebhookStatus status, LocalDateTime before);

    List<WebhookDelivery> findByPaymentId(UUID paymentId);

    List<WebhookDelivery> findByStatus(WebhookStatus status);
}
