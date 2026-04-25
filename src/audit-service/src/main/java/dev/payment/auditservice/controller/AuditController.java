package dev.payment.auditservice.controller;

import dev.payment.auditservice.entity.AuditLog;
import dev.payment.auditservice.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
    private final AuditRepository auditRepository;

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<AuditLog>> getPaymentHistory(@PathVariable String paymentId) {
        return ResponseEntity.ok(auditRepository.findByAggregateIdOrderByTimestampAsc(paymentId));
    }
}
