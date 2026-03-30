package dev.payment.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.notificationservice.domain.Channel;
import dev.payment.notificationservice.domain.ConsumedEvent;
import dev.payment.notificationservice.domain.DeliveryStatus;
import dev.payment.notificationservice.domain.Notification;
import dev.payment.notificationservice.repository.ConsumedEventRepository;
import dev.payment.notificationservice.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.handler.annotation.Header;

@Component
public class PaymentEventListener {

    private final NotificationRepository notificationRepository;
    private final ConsumedEventRepository consumedEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(
            NotificationRepository notificationRepository,
            ConsumedEventRepository consumedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.consumedEventRepository = consumedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${application.kafka.topic.payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(PaymentEventMessage message) {
        try {
            if (consumedEventRepository.existsByEventId(message.eventId().toString())) {
                return;
            }
            persistNotification(message, DeliveryStatus.DELIVERED, "notification-service");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist payment event notification", exception);
        }
    }

    @Transactional
    @KafkaListener(topics = "${application.kafka.topic.payment-events-dlt}", groupId = "${spring.kafka.consumer.group-id}.dlt")
    public void onDeadLetterEvent(
            PaymentEventMessage message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        if (consumedEventRepository.existsByEventId(message.eventId().toString())) {
            return;
        }
        persistNotification(message, DeliveryStatus.FAILED, "notification-service-dlt:" + topic);
    }

    private void persistNotification(PaymentEventMessage message, DeliveryStatus status, String consumerName) {
        Notification notification = new Notification();
        notification.setRecipientAddress("ops@nova.local");
        notification.setTemplateCode(message.eventType().toUpperCase().replace('.', '_'));
        notification.setChannel(Channel.WEBHOOK);
        notification.setStatus(status);
        notification.setPayload(writePayload(message));
        notificationRepository.save(notification);

        ConsumedEvent consumedEvent = new ConsumedEvent();
        consumedEvent.setConsumerName(consumerName);
        consumedEvent.setEventId(message.eventId().toString());
        consumedEventRepository.save(consumedEvent);
    }

    private String writePayload(PaymentEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize payment event", exception);
        }
    }
}
