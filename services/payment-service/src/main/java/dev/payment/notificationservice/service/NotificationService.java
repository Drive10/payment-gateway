package dev.payment.notificationservice.service;

import dev.payment.notificationservice.domain.DeliveryStatus;
import dev.payment.notificationservice.domain.Notification;
import dev.payment.notificationservice.domain.Template;
import dev.payment.notificationservice.dto.request.CreateTemplateRequest;
import dev.payment.notificationservice.dto.request.SendNotificationRequest;
import dev.payment.notificationservice.dto.response.NotificationResponse;
import dev.payment.notificationservice.dto.response.TemplateResponse;
import dev.payment.notificationservice.repository.NotificationRepository;
import dev.payment.notificationservice.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    private final TemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(TemplateRepository templateRepository, NotificationRepository notificationRepository) {
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
    }

    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        Template template = new Template();
        template.setTemplateCode(request.templateCode().toUpperCase());
        template.setChannel(request.channel());
        template.setSubject(request.subject());
        template.setBody(request.body());
        templateRepository.save(template);
        return new TemplateResponse(template.getId(), template.getTemplateCode(), template.getChannel().name(), template.getSubject(), template.getBody());
    }

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        templateRepository.findByTemplateCode(request.templateCode().toUpperCase()).orElseThrow();
        Notification notification = new Notification();
        notification.setRecipientAddress(request.recipientAddress());
        notification.setTemplateCode(request.templateCode().toUpperCase());
        notification.setChannel(request.channel());
        notification.setPayload(request.payload());
        notification.setStatus(DeliveryStatus.QUEUED);
        notificationRepository.save(notification);
        notification.setStatus(DeliveryStatus.DISPATCHED);
        notification.setStatus(DeliveryStatus.DELIVERED);
        return new NotificationResponse(notification.getId(), notification.getRecipientAddress(), notification.getTemplateCode(), notification.getChannel().name(), notification.getStatus().name(), notification.getPayload(), notification.getCreatedAt());
    }

    public List<NotificationResponse> getDelivered() {
        return notificationRepository.findByStatus(DeliveryStatus.DELIVERED).stream()
                .map(notification -> new NotificationResponse(notification.getId(), notification.getRecipientAddress(), notification.getTemplateCode(), notification.getChannel().name(), notification.getStatus().name(), notification.getPayload(), notification.getCreatedAt()))
                .toList();
    }
}
