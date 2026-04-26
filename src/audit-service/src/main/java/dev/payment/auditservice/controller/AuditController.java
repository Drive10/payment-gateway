package dev.payment.auditservice.controller;

import dev.payment.auditservice.entity.AuditLog;
import dev.payment.auditservice.repository.AuditRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "API for retrieving audit logs")
public class AuditController {
    private final AuditRepository auditRepository;

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "Get payment audit history", description = "Retrieve audit logs for a payment")
    public ResponseEntity<List<AuditLog>> getPaymentHistory(@PathVariable String paymentId) {
        return ResponseEntity.ok(auditRepository.findByAggregateIdOrderByTimestampAsc(paymentId));
    }
}
