package dev.payment.notificationservice.service;

import dev.payment.notificationservice.dto.NotificationEvent;
import dev.payment.notificationservice.dto.SendNotificationRequest;
import dev.payment.notificationservice.entity.Notification;
import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.NotificationStatus;
import dev.payment.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification sendNotification(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .channel(request.getChannel())
                .recipient(request.getRecipient())
                .subject(request.getSubject())
                .content(request.getContent())
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", notification.getId());

        return switch (request.getChannel()) {
            case EMAIL -> sendEmail(notification);
            case SMS -> sendSms(notification);
            case PUSH -> logNotification(notification);
        };
    }

    public Notification sendNotification(NotificationEvent event) {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .channel(event.getChannel())
                .recipient(event.getRecipient())
                .subject(event.getSubject())
                .content(event.getContent())
                .build();
        return sendNotification(request);
    }

    private Notification sendEmail(Notification notification) {
        log.info("Sending email to: {} with subject: {}", 
                notification.getRecipient(), notification.getSubject());
        log.debug("Email content: {}", notification.getContent());
        
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    private Notification sendSms(Notification notification) {
        log.info("Sending SMS to: {}", notification.getRecipient());
        log.debug("SMS content: {}", notification.getContent());
        
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    private Notification logNotification(Notification notification) {
        log.info("Push notification logged for user: {}", notification.getUserId());
        log.debug("Push content: {}", notification.getContent());
        
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
}
