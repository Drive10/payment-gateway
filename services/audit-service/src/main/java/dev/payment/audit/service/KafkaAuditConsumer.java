package dev.payment.audit.service;

import dev.payment.audit.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaAuditConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = KafkaConfig.AUDIT_EVENTS_TOPIC, groupId = "audit-service-group")
    public void consumeAuditEvent(@Payload Map<String, Object> event) {
        try {
            String userId = (String) event.get("userId");
            String entityType = (String) event.get("entityType");
            String entityId = (String) event.get("entityId");
            String action = (String) event.get("action");
            String service = (String) event.get("service");
            Boolean success = (Boolean) event.getOrDefault("success", true);

            auditService.createLog(userId, entityType, entityId, action, service, success);
            log.debug("Processed audit event: {} for {} {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", event, e);
        }
    }
}
