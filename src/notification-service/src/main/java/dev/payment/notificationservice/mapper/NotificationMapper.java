package dev.payment.notificationservice.mapper;

import dev.payment.notificationservice.dto.NotificationResponse;
import dev.payment.notificationservice.dto.TemplateResponse;
import dev.payment.notificationservice.entity.Notification;
import dev.payment.notificationservice.entity.Template;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
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

    public TemplateResponse toTemplateResponse(Template template) {
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
