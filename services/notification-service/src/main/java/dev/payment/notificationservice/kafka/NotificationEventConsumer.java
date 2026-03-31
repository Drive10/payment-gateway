package dev.payment.notificationservice.kafka;

import dev.payment.notificationservice.dto.NotificationEvent;
import dev.payment.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "payment.events",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {
                    "spring.json.value.default.type:dev.payment.notificationservice.dto.NotificationEvent"
            }
    )
    public void consume(NotificationEvent event) {
        log.info("Received notification event for user: {}, type: {}", 
                event.getUserId(), event.getType());

        try {
            notificationService.sendNotification(event);
            log.info("Successfully processed notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process notification for user: {}, error: {}", 
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}
