package dev.payment.notificationservice.kafka;

import dev.payment.common.events.PaymentEventMessage;
import dev.payment.notificationservice.dto.NotificationEvent;
import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.NotificationType;
import dev.payment.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private final NotificationService notificationService;

    @Value("${application.kafka.topic.payment-events:payment.events}")
    private String paymentEventsTopic;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${application.kafka.topic.payment-events:payment.events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(PaymentEventMessage event) {
        log.info("Received payment event: type={}, paymentId={}, status={}",
                event.eventType(), event.paymentId(), event.paymentStatus());

        try {
            NotificationEvent notificationEvent = mapToNotificationEvent(event);
            notificationService.sendNotification(notificationEvent);
            log.info("Successfully processed notification for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Failed to process payment event for paymentId: {}, error: {}",
                    event.paymentId(), e.getMessage(), e);
        }
    }

    private NotificationEvent mapToNotificationEvent(PaymentEventMessage event) {
        NotificationType type = switch (event.eventType()) {
            case "payment.created", "payment.initiated" -> NotificationType.PAYMENT_PENDING;
            case "payment.captured", "payment.webhook.captured" -> NotificationType.PAYMENT_SUCCESS;
            case "payment.failed", "payment.webhook.failed" -> NotificationType.PAYMENT_FAILED;
            case "payment.refunded", "payment.webhook.refund_processed" -> NotificationType.REFUND_PROCESSED;
            default -> NotificationType.PAYMENT_SUCCESS;
        };

        String userIdStr = event.metadata() != null ? event.metadata().get("userId") : null;
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

        return NotificationEvent.builder()
                .userId(userId)
                .type(type)
                .channel(NotificationChannel.EMAIL)
                .recipient(event.metadata() != null ? event.metadata().get("email") : "unknown")
                .subject("Payment " + event.paymentStatus())
                .content("Payment " + event.paymentId() + " for order " + event.orderReference() + " is now " + event.paymentStatus())
                .build();
    }
}
