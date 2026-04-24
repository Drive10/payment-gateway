package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.domain.entities.NotificationChannel;
import dev.payment.notificationservice.domain.entities.NotificationType;

import java.util.UUID;

public class NotificationEvent {

    private UUID userId;
    private NotificationType type;
    private NotificationChannel channel;
    private String recipient;
    private String subject;
    private String content;

    public NotificationEvent() {}

    public NotificationEvent(UUID userId, NotificationType type, NotificationChannel channel,
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

    public static NotificationEventBuilder builder() { return new NotificationEventBuilder(); }

    public static class NotificationEventBuilder {
        private UUID userId;
        private NotificationType type;
        private NotificationChannel channel;
        private String recipient;
        private String subject;
        private String content;

        public NotificationEventBuilder userId(UUID userId) { this.userId = userId; return this; }
        public NotificationEventBuilder type(NotificationType type) { this.type = type; return this; }
        public NotificationEventBuilder channel(NotificationChannel channel) { this.channel = channel; return this; }
        public NotificationEventBuilder recipient(String recipient) { this.recipient = recipient; return this; }
        public NotificationEventBuilder subject(String subject) { this.subject = subject; return this; }
        public NotificationEventBuilder content(String content) { this.content = content; return this; }
        public NotificationEvent build() {
            return new NotificationEvent(userId, type, channel, recipient, subject, content);
        }
    }
}
