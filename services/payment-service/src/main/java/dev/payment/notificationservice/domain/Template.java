package dev.payment.notificationservice.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "templates")
public class Template extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 64)
    private String templateCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Channel channel;
    @Column(nullable = false, length = 120)
    private String subject;
    @Column(nullable = false, length = 2048)
    private String body;
    public UUID getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
