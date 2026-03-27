package dev.payment.notificationservice.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "recipient_address", nullable = false, length = 255)
    private String recipientAddress;
    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Channel channel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status;
    @Column(nullable = false, length = 2048)
    private String payload;
    public UUID getId() { return id; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String recipientAddress) { this.recipientAddress = recipientAddress; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
