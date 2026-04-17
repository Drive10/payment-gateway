package dev.payment.notificationservice.service;

import dev.payment.notificationservice.dto.SmsRequest;
import dev.payment.notificationservice.dto.NotificationResponse;
import dev.payment.notificationservice.entity.Notification;
import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.NotificationStatus;
import dev.payment.notificationservice.entity.NotificationType;
import dev.payment.notificationservice.mapper.NotificationMapper;
import dev.payment.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    public SmsService(NotificationRepository notificationRepository, NotificationMapper mapper) {
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
    }

    @Transactional
    public NotificationResponse send(SmsRequest request) {
        Notification notification = createNotification(
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.SMS,
                request.to(),
                null,
                request.message()
        );
        log.info("notification=sms_sent id={} recipient={}", notification.getId(), request.to());
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
