package dev.payment.notificationservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebhookEvent {
    private String eventId;
    private String eventType;
    private Object payload;
    private long timestamp;
}
