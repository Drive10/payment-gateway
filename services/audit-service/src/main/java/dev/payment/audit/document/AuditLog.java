package dev.payment.audit.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "entity_time_idx", def = "{'entityType': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'timestamp': -1}")
})
public class AuditLog {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String entityType;

    private String entityId;

    private String action;

    private String service;

    @Indexed
    private Instant timestamp;

    @CreatedDate
    private Instant createdAt;

    private String ipAddress;

    private String userAgent;

    private Map<String, Object> oldValue;

    private Map<String, Object> newValue;

    private boolean success;

    private String errorMessage;
}
