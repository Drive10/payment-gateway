package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.AuditLog;
import dev.payment.paymentservice.payment.dto.response.AuditLogResponse;
import dev.payment.paymentservice.payment.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String action, String actor, String resourceType, String resourceId, String summary) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setActor(actor);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setSummary(summary);
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getActor(),
                        log.getResourceType(),
                        log.getResourceId(),
                        log.getSummary(),
                        log.getCreatedAt()
                ));
    }
}
