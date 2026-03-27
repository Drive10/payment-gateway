package dev.payment.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.notificationservice.domain.Channel;
import dev.payment.notificationservice.domain.DeliveryStatus;
import dev.payment.notificationservice.domain.Notification;
import dev.payment.notificationservice.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${application.kafka.topic.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(PaymentEventMessage message) {
        try {
            Notification notification = new Notification();
            notification.setRecipientAddress("ops@nova.local");
            notification.setTemplateCode(message.eventType().toUpperCase().replace('.', '_'));
            notification.setChannel(Channel.WEBHOOK);
            notification.setStatus(DeliveryStatus.DELIVERED);
            notification.setPayload(objectMapper.writeValueAsString(message));
            notificationRepository.save(notification);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist payment event notification", exception);
        }
    }
}
