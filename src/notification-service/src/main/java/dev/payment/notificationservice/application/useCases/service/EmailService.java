package dev.payment.notificationservice.service;

import dev.payment.notificationservice.dto.EmailRequest;
import dev.payment.notificationservice.dto.NotificationResponse;
import dev.payment.notificationservice.domain.entities.Notification;
import dev.payment.notificationservice.domain.entities.NotificationChannel;
import dev.payment.notificationservice.domain.entities.NotificationStatus;
import dev.payment.notificationservice.domain.entities.NotificationType;
import dev.payment.notificationservice.mapper.NotificationMapper;
import dev.payment.notificationservice.infrastructure.persistence.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    public EmailService(NotificationRepository notificationRepository, NotificationMapper mapper) {
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
    }

    @Transactional
    public NotificationResponse send(EmailRequest request) {
        Notification notification = createNotification(
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.EMAIL,
                request.to(),
                request.subject(),
                request.body()
        );
        log.info("notification=email_sent id={} recipient={}", notification.getId(), request.to());
        return mapper.toResponse(notification);
    }

    private Notification createNotification(NotificationType type, NotificationChannel channel,
                                          String recipient, String subject, String content) {
        Notification notification = Notification.builder()
                .userId(java.util.UUID.randomUUID())
                .type(type)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .build();
        notification = notificationRepository.save(notification);

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }
}
