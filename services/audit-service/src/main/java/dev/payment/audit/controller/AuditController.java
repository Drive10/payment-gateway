package dev.payment.audit.controller;

import dev.payment.audit.document.AuditLog;
import dev.payment.audit.dto.AuditLogResponse;
import dev.payment.audit.dto.CreateAuditLogRequest;
import dev.payment.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final String FIELD_TIMESTAMP = "timestamp";

    private final AuditService auditService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getLogsByUser(userId, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP)));
        return ResponseEntity.ok(logs.map(AuditLogResponse::from));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLogResponse>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getLogsByEntity(entityType, entityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP)));
        return ResponseEntity.ok(logs.map(AuditLogResponse::from));
    }

    @GetMapping("/service/{service}")
    public ResponseEntity<Page<AuditLogResponse>> getByService(
            @PathVariable String service,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getLogsByService(service,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP)));
        return ResponseEntity.ok(logs.map(AuditLogResponse::from));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditLogResponse>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        List<AuditLogResponse> logs = auditService.getLogsBetween(start, end)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam String action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.searchLogs(action, start, end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP)));
        return ResponseEntity.ok(logs.map(AuditLogResponse::from));
    }

    @PostMapping
    public ResponseEntity<AuditLogResponse> create(@RequestBody CreateAuditLogRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .userId(request.userId())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .action(request.action())
                .service(request.service())
                .timestamp(request.timestamp())
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .oldValue(request.oldValue())
                .newValue(request.newValue())
                .success(request.success())
                .errorMessage(request.errorMessage())
                .build();
        AuditLog created = auditService.log(auditLog);
        return ResponseEntity.ok(AuditLogResponse.from(created));
    }
}
