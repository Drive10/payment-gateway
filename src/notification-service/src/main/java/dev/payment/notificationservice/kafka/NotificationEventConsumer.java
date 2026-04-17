package dev.payment.notificationservice.kafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.payment.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true"
    )
    @KafkaListener(topics = "${application.kafka.topic.notification-events:notification.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeNotificationEvent(@Payload String message,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          Acknowledgment ack) {
        try {
            log.info("Processing notification event from {}: {}", topic, message);
            // Event processing handled by downstream services
            ack.acknowledge();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification event: {}", e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process notification event. Message: {}. Error: {}", message, e.getMessage(), e);
            throw e;
        }
    }

    @DltHandler
    public void handleDeadLetter(@Payload String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Message moved to DLT from {}: {}", topic, message);
    }
}
