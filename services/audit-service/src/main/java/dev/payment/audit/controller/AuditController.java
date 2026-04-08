package dev.payment.audit.controller;

import dev.payment.audit.document.AuditLog;
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
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsByUser(userId, 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP))));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsByEntity(entityType, entityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP))));
    }

    @GetMapping("/service/{service}")
    public ResponseEntity<Page<AuditLog>> getByService(
            @PathVariable String service,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getLogsByService(service,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP))));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        return ResponseEntity.ok(auditService.getLogsBetween(start, end));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AuditLog>> search(
            @RequestParam String action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.searchLogs(action, start, end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, FIELD_TIMESTAMP))));
    }

    @PostMapping
    public ResponseEntity<AuditLog> create(@RequestBody AuditLog auditLog) {
        return ResponseEntity.ok(auditService.log(auditLog));
    }
}
