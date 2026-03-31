package dev.payment.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void logCreate(String entityType, String entityId, String userId, Object newValue) {
        log.info("AUDIT: CREATE | entity={} | id={} | user={} | value={} | correlationId={}",
                entityType, entityId, userId, newValue, MDC.get("correlationId"));
    }

    public void logUpdate(String entityType, String entityId, String userId, Object oldValue, Object newValue) {
        log.info("AUDIT: UPDATE | entity={} | id={} | user={} | oldValue={} | newValue={} | correlationId={}",
                entityType, entityId, userId, oldValue, newValue, MDC.get("correlationId"));
    }

    public void logDelete(String entityType, String entityId, String userId) {
        log.info("AUDIT: DELETE | entity={} | id={} | user={} | correlationId={}",
                entityType, entityId, userId, MDC.get("correlationId"));
    }

    public void logAccess(String entityType, String entityId, String userId, String action) {
        log.info("AUDIT: ACCESS | entity={} | id={} | user={} | action={} | correlationId={}",
                entityType, entityId, userId, action, MDC.get("correlationId"));
    }

    public void logPayment(String userId, String paymentId, String action, String status) {
        log.info("AUDIT: PAYMENT | user={} | paymentId={} | action={} | status={} | correlationId={}",
                userId, paymentId, action, status, MDC.get("correlationId"));
    }

    public void logSecurity(String userId, String action, String details) {
        log.warn("AUDIT: SECURITY | user={} | action={} | details={} | correlationId={}",
                userId, action, details, MDC.get("correlationId"));
    }
}
