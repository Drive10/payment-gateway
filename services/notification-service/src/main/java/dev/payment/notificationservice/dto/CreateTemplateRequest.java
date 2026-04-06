package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;

public class CreateTemplateRequest {
    
    @NotBlank(message = "Template key is required")
    private String templateKey;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Channel is required")
    private NotificationChannel channel;
    
    private String subject;
    
    @NotBlank(message = "Body template is required")
    private String bodyTemplate;
    
    private String eventType;

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
}
