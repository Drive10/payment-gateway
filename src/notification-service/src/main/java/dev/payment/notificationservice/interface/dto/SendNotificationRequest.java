package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.domain.entities.NotificationChannel;
import dev.payment.notificationservice.domain.entities.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class SendNotificationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel channel;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    public SendNotificationRequest() {}

    public SendNotificationRequest(UUID userId, NotificationType type, NotificationChannel channel,
                                  String recipient, String subject, String content) {
        this.userId = userId;
        this.type = type;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public static SendNotificationRequestBuilder builder() { return new SendNotificationRequestBuilder(); }

    public static class SendNotificationRequestBuilder {
        private UUID userId;
        private NotificationType type;
        private NotificationChannel channel;
        private String recipient;
        private String subject;
        private String content;

        public SendNotificationRequestBuilder userId(UUID userId) { this.userId = userId; return this; }
        public SendNotificationRequestBuilder type(NotificationType type) { this.type = type; return this; }
        public SendNotificationRequestBuilder channel(NotificationChannel channel) { this.channel = channel; return this; }
        public SendNotificationRequestBuilder recipient(String recipient) { this.recipient = recipient; return this; }
        public SendNotificationRequestBuilder subject(String subject) { this.subject = subject; return this; }
        public SendNotificationRequestBuilder content(String content) { this.content = content; return this; }
        public SendNotificationRequest build() {
            return new SendNotificationRequest(userId, type, channel, recipient, subject, content);
        }
    }
}
