package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.AuditLog;
import dev.payment.paymentservice.dto.response.AuditLogResponse;
import dev.payment.paymentservice.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

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
