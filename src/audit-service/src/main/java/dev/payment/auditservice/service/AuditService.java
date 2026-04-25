package dev.payment.auditservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.auditservice.entity.AuditLog;
import dev.payment.auditservice.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"payment.payment_created", "payment.payment_status_updated"}, groupId = "audit-group")
    public void handlePaymentEvent(String message) {
        try {
            log.info("Raw message received: {}", message);
            
            // Handle double-encoded JSON - strips outer quotes if present
            String jsonToParse = message;
            if (message != null && message.startsWith("\"") && message.endsWith("\"")) {
                jsonToParse = message.substring(1, message.length() - 1);
                jsonToParse = jsonToParse.replace("\\\"", "\"");
            }
            
            Map<String, Object> eventData = objectMapper.readValue(jsonToParse, Map.class);
            String paymentId = (String) eventData.get("paymentId");
            
            AuditLog logEntry = AuditLog.builder()
                .aggregateId(paymentId)
                .eventType("PAYMENT_EVENT")
                .payload(jsonToParse) // Store the actual JSON string
                .timestamp(Instant.now())
                .build();
            
            auditRepository.save(logEntry);
            log.info("Audit log recorded for payment: {}", paymentId);
        } catch (Exception e) {
            log.error("Error recording audit log: {}", e.getMessage());
        }
    }
}
