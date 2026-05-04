package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.service.LedgerService;
import dev.payment.paymentservice.service.SettlementService;
import dev.payment.paymentservice.entity.LedgerEntry;
import dev.payment.paymentservice.entity.SettlementBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final LedgerService ledgerService;
    private final SettlementService settlementService;

    @GetMapping("/merchants/{merchantId}/balance")
    public ResponseEntity<Map<String, Object>> getMerchantBalance(@PathVariable String merchantId) {
        BigDecimal balance = ledgerService.getMerchantBalance(merchantId);
        BigDecimal pending = settlementService.getMerchantPendingSettlement(merchantId);
        BigDecimal settled = settlementService.getMerchantSettledAmount(merchantId);
        
        return ResponseEntity.ok(Map.of(
                "merchantId", merchantId,
                "availableBalance", balance.subtract(pending),
                "pendingSettlement", pending,
                "totalSettled", settled,
                "ledgerBalance", balance
        ));
    }

    @GetMapping("/merchants/{merchantId}/ledger")
    public ResponseEntity<List<LedgerEntry>> getMerchantLedger(@PathVariable String merchantId) {
        return ResponseEntity.ok(ledgerService.getMerchantLedger(merchantId));
    }

    @GetMapping("/merchants/{merchantId}/settlements")
    public ResponseEntity<List<SettlementBatch>> getMerchantSettlements(@PathVariable String merchantId) {
        return ResponseEntity.ok(settlementService.getMerchantSettlements(merchantId));
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementBatch> createSettlement(@RequestBody Map<String, String> request) {
        String merchantId = request.get("merchantId");
        String currency = request.getOrDefault("currency", "USD");
        
        SettlementBatch batch = settlementService.createSettlementBatch(merchantId, currency);
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/settlements/{batchId}/approve")
    public ResponseEntity<SettlementBatch> approveSettlement(@PathVariable String batchId) {
        SettlementBatch batch = settlementService.approveBatch(java.util.UUID.fromString(batchId));
        return ResponseEntity.ok(batch);
    }

    @PostMapping("/settlements/{batchId}/payout")
    public ResponseEntity<SettlementBatch> initiatePayout(@PathVariable String batchId, @RequestBody Map<String, String> request) {
        String payoutMethod = request.get("payoutMethod");
        SettlementBatch batch = settlementService.initiatePayout(java.util.UUID.fromString(batchId), payoutMethod);
        return ResponseEntity.ok(batch);
    }

    @GetMapping("/reports/daily")
    public ResponseEntity<Map<String, Object>> getDailyReport(@RequestParam(required = false) String date) {
        Instant from = Instant.parse(date + "T00:00:00Z");
        Instant to = from.plus(24, ChronoUnit.HOURS);
        
        List<dev.payment.paymentservice.entity.Journal> journals = ledgerService.getPostedJournals(from, to);
        
        BigDecimal totalDebits = journals.stream()
                .map(dev.payment.paymentservice.entity.Journal::getTotalDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = journals.stream()
                .map(dev.payment.paymentservice.entity.Journal::getTotalCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return ResponseEntity.ok(Map.of(
                "date", date,
                "journalCount", journals.size(),
                "totalDebits", totalDebits,
                "totalCredits", totalCredits,
                "netBalance", totalDebits.subtract(totalCredits),
                "isBalanced", totalDebits.compareTo(totalCredits) == 0
        ));
    }

    @GetMapping("/integrity/check")
    public ResponseEntity<Map<String, Object>> checkIntegrity() {
        boolean balanced = ledgerService.validateLedgerBalance();
        return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now(),
                "balanced", balanced,
                "status", balanced ? "PASS" : "FAIL"
        ));
    }
}