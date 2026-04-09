package dev.payment.notificationservice.service;

import dev.payment.notificationservice.dto.*;
import dev.payment.notificationservice.entity.Notification;
import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.NotificationStatus;
import dev.payment.notificationservice.entity.NotificationType;
import dev.payment.notificationservice.entity.Template;
import dev.payment.notificationservice.repository.NotificationRepository;
import dev.payment.notificationservice.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final TemplateRepository templateRepository;

    public NotificationService(NotificationRepository notificationRepository, TemplateRepository templateRepository) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional
    public NotificationResponse sendEmail(EmailRequest request) {
        Notification notification = createNotification(
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.EMAIL,
                request.to(),
                request.subject(),
                request.body()
        );
        log.info("notification=email_sent id={} recipient={}", notification.getId(), request.to());
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse sendSms(SmsRequest request) {
        Notification notification = createNotification(
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.SMS,
                request.to(),
                null,
                request.message()
        );
        log.info("notification=sms_sent id={} recipient={}", notification.getId(), request.to());
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse sendWebhook(WebhookRequest request) {
        Notification notification = Notification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.WEBHOOK)
                .status(NotificationStatus.PENDING)
                .recipient(request.url())
                .subject(request.eventType())
                .payload(request.payload() != null ? request.payload().toString() : null)
                .eventType(request.eventType())
                .build();

        notification = notificationRepository.save(notification);
        log.info("notification=webhook_sent id={} url={}", notification.getId(), request.url());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        return toResponse(notificationRepository.save(notification));
    }

    public Optional<NotificationResponse> findById(UUID id) {
        return notificationRepository.findById(id).map(this::toResponse);
    }

    public List<NotificationResponse> findAll(String userId, String status, int page, int size) {
        List<Notification> notifications = findNotificationsByFilter(userId, status);
        return notifications.stream().map(this::toResponse).toList();
    }

    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        Template template = Template.builder()
                .templateKey(request.templateKey())
                .name(request.name())
                .description(request.description())
                .channel(request.channel())
                .subject(request.subject())
                .bodyTemplate(request.bodyTemplate())
                .eventType(request.eventType())
                .active(true)
                .build();
        template = templateRepository.save(template);
        log.info("template=created id={} key={}", template.getId(), template.getTemplateKey());
        return toTemplateResponse(template);
    }

    public Optional<TemplateResponse> updateTemplate(UUID id, UpdateTemplateRequest request) {
        return templateRepository.findById(id).map(template -> {
            if (request.name() != null) template.setName(request.name());
            if (request.subject() != null) template.setSubject(request.subject());
            if (request.bodyTemplate() != null) template.setBodyTemplate(request.bodyTemplate());
            if (request.description() != null) template.setDescription(request.description());
            if (request.active() != null) template.setActive(request.active());
            return toTemplateResponse(templateRepository.save(template));
        });
    }

    @Transactional
    public Optional<NotificationResponse> retry(UUID id) {
        return notificationRepository.findById(id).map(notification -> {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification = notificationRepository.save(notification);
            log.info("notification=retry id={} attempt={}", id, notification.getRetryCount());

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(java.time.LocalDateTime.now());
            return toResponse(notificationRepository.save(notification));
        });
    }

    public Notification sendNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.getUserId() != null ? event.getUserId() : UUID.randomUUID())
                .type(event.getType())
                .channel(event.getChannel())
                .status(NotificationStatus.PENDING)
                .recipient(event.getRecipient())
                .subject(event.getSubject())
                .content(event.getContent())
                .build();
        return notificationRepository.save(notification);
    }

    private Notification createNotification(NotificationType type, NotificationChannel channel,
                                          String recipient, String subject, String content) {
        Notification notification = Notification.builder()
                .userId(UUID.randomUUID())
                .type(type)
                .channel(channel)
                .status(NotificationStatus.PENDING)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .build();
        notification = notificationRepository.save(notification);

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    private List<Notification> findNotificationsByFilter(String userId, String status) {
        if (userId != null && status != null) {
            return notificationRepository.findByUserIdAndStatus(
                    UUID.fromString(userId),
                    NotificationStatus.valueOf(status.toUpperCase())
            );
        }
        if (userId != null) {
            return notificationRepository.findByUserId(UUID.fromString(userId));
        }
        if (status != null) {
            return notificationRepository.findByStatus(NotificationStatus.valueOf(status.toUpperCase()));
        }
        return notificationRepository.findAll();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType() != null ? notification.getType().name() : null,
                notification.getChannel(),
                notification.getStatus(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent(),
                notification.getEventType(),
                null,
                notification.getRetryCount(),
                notification.getLastError(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }

    private TemplateResponse toTemplateResponse(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getTemplateKey(),
                template.getName(),
                template.getDescription(),
                template.getChannel(),
                template.getSubject(),
                template.getBodyTemplate(),
                template.getEventType(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
