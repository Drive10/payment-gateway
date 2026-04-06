package dev.payment.notificationservice.dto;

public class UpdateTemplateRequest {
    
    private String name;
    private String description;
    private String subject;
    private String bodyTemplate;
    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
