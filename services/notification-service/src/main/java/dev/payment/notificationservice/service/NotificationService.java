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
import java.util.Map;
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
        Notification notification = Notification.builder()
                .userId(request.getUserId() != null ? UUID.fromString(request.getUserId()) : UUID.randomUUID())
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient(request.getTo())
                .subject(request.getSubject())
                .content(request.getBody())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Email notification created with ID: {}", notification.getId());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        notification = notificationRepository.save(notification);

        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse sendSms(SmsRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId() != null ? UUID.fromString(request.getUserId()) : UUID.randomUUID())
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.SMS)
                .status(NotificationStatus.PENDING)
                .recipient(request.getTo())
                .content(request.getMessage())
                .build();

        notification = notificationRepository.save(notification);
        log.info("SMS notification created with ID: {}", notification.getId());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        notification = notificationRepository.save(notification);

        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse sendWebhook(WebhookRequest request) {
        Notification notification = Notification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.WEBHOOK)
                .status(NotificationStatus.PENDING)
                .recipient(request.getUrl())
                .subject(request.getEventType())
                .payload(request.getPayload() != null ? request.getPayload().toString() : null)
                .eventType(request.getEventType())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Webhook notification created with ID: {}", notification.getId());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(java.time.LocalDateTime.now());
        notification = notificationRepository.save(notification);

        return toResponse(notification);
    }

    public Optional<NotificationResponse> findById(UUID id) {
        return notificationRepository.findById(id).map(this::toResponse);
    }

    public List<NotificationResponse> findAll(String userId, String status, int page, int size) {
        List<Notification> notifications;
        if (userId != null && status != null) {
            notifications = notificationRepository.findByUserIdAndStatus(
                    UUID.fromString(userId),
                    NotificationStatus.valueOf(status.toUpperCase())
            );
        } else if (userId != null) {
            notifications = notificationRepository.findByUserId(UUID.fromString(userId));
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(NotificationStatus.valueOf(status.toUpperCase()));
        } else {
            notifications = notificationRepository.findAll();
        }
        return notifications.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TemplateResponse> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toTemplateResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        Template template = Template.builder()
                .templateKey(request.getTemplateKey())
                .name(request.getName())
                .description(request.getDescription())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .eventType(request.getEventType())
                .active(true)
                .build();
        template = templateRepository.save(template);
        return toTemplateResponse(template);
    }

    public Optional<TemplateResponse> updateTemplate(UUID id, UpdateTemplateRequest request) {
        return templateRepository.findById(id).map(template -> {
            if (request.getName() != null) template.setName(request.getName());
            if (request.getSubject() != null) template.setSubject(request.getSubject());
            if (request.getBodyTemplate() != null) template.setBodyTemplate(request.getBodyTemplate());
            if (request.getDescription() != null) template.setDescription(request.getDescription());
            if (request.getActive() != null) template.setActive(request.getActive());
            return toTemplateResponse(templateRepository.save(template));
        });
    }

    @Transactional
    public Optional<NotificationResponse> retry(UUID id) {
        return notificationRepository.findById(id).map(notification -> {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification = notificationRepository.save(notification);
            log.info("Retrying notification with ID: {}", id);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(java.time.LocalDateTime.now());
            notification = notificationRepository.save(notification);

            return toResponse(notification);
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

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setUserId(notification.getUserId());
        response.setType(notification.getType() != null ? notification.getType().name() : null);
        response.setChannel(notification.getChannel());
        response.setStatus(notification.getStatus());
        response.setRecipient(notification.getRecipient());
        response.setSubject(notification.getSubject());
        response.setBody(notification.getContent());
        response.setEventType(notification.getEventType());
        response.setRetryCount(notification.getRetryCount());
        response.setLastError(notification.getLastError());
        response.setCreatedAt(notification.getCreatedAt());
        response.setSentAt(notification.getSentAt());
        return response;
    }

    private TemplateResponse toTemplateResponse(Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setTemplateKey(template.getTemplateKey());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setChannel(template.getChannel());
        response.setSubject(template.getSubject());
        response.setBodyTemplate(template.getBodyTemplate());
        response.setEventType(template.getEventType());
        response.setActive(template.isActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
