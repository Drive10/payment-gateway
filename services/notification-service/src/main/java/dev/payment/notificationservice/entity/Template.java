package dev.payment.notificationservice.entity;
import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates", indexes = {
    @Index(name = "idx_template_key", columnList = "templateKey", unique = true),
    @Index(name = "idx_template_channel", columnList = "channel")
})
@Data
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String templateKey;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    private String eventType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static TemplateBuilder builder() { return new TemplateBuilder(); }

    public static class TemplateBuilder {
        private String templateKey;
        private String name;
        private String description;
        private NotificationChannel channel;
        private String subject;
        private String bodyTemplate;
        private String eventType;
        private boolean active = true;

        public TemplateBuilder templateKey(String templateKey) { this.templateKey = templateKey; return this; }
        public TemplateBuilder name(String name) { this.name = name; return this; }
        public TemplateBuilder description(String description) { this.description = description; return this; }
        public TemplateBuilder channel(NotificationChannel channel) { this.channel = channel; return this; }
        public TemplateBuilder subject(String subject) { this.subject = subject; return this; }
        public TemplateBuilder bodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; return this; }
        public TemplateBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public TemplateBuilder active(boolean active) { this.active = active; return this; }
        public Template build() {
            Template template = new Template();
            template.templateKey = templateKey;
            template.name = name;
            template.description = description;
            template.channel = channel;
            template.subject = subject;
            template.bodyTemplate = bodyTemplate;
            template.eventType = eventType;
            template.active = active;
            return template;
        }
    }
}
