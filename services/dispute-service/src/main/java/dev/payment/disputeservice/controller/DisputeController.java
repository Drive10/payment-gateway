package dev.payment.disputeservice.controller;

import dev.payment.disputeservice.entity.Dispute;
import dev.payment.disputeservice.service.DisputeService;
import dev.payment.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Dispute>> createDispute(@RequestBody Map<String, Object> request) {
        validateCreateRequest(request);

        Dispute dispute = new Dispute();
        dispute.setPaymentId(getStringRequired(request, "paymentId"));
        dispute.setOrderId(getString(request, "orderId"));
        dispute.setMerchantId(UUID.fromString(getStringRequired(request, "merchantId")));
        dispute.setCustomerId(getString(request, "customerId"));
        dispute.setAmount(new BigDecimal(getStringRequired(request, "amount")));
        dispute.setCurrency(getStringOrDefault(request, "currency", "INR"));
        dispute.setDisputeReason(getStringRequired(request, "disputeReason"));
        dispute.setDisputeType(getStringRequired(request, "disputeType"));
        dispute.setPriority(getStringOrDefault(request, "priority", "NORMAL"));

        Dispute created = disputeService.createDispute(dispute);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Dispute>> getDispute(@PathVariable UUID id) {
        return disputeService.getDispute(id)
                .map(d -> ResponseEntity.ok(ApiResponse.success(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-dispute-id/{disputeId}")
    public ResponseEntity<ApiResponse<Dispute>> getDisputeByDisputeId(@PathVariable String disputeId) {
        return disputeService.getDisputeByDisputeId(disputeId)
                .map(d -> ResponseEntity.ok(ApiResponse.success(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-payment/{paymentId}")
    public ResponseEntity<ApiResponse<Dispute>> getDisputeByPaymentId(@PathVariable String paymentId) {
        return disputeService.getDisputeByPaymentId(paymentId)
                .map(d -> ResponseEntity.ok(ApiResponse.success(d)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<Dispute>>> getDisputesByMerchant(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) String status) {

        List<Dispute> disputes;
        if (status != null) {
            disputes = disputeService.getDisputesByMerchantAndStatus(merchantId, status);
        } else {
            disputes = disputeService.getDisputesByMerchant(merchantId);
        }
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Dispute>>> getAllDisputes(
            @RequestParam(required = false) String status) {

        List<Dispute> disputes;
        if (status != null) {
            disputes = disputeService.getDisputesByStatus(status);
        } else {
            disputes = disputeService.getAllDisputes();
        }
        return ResponseEntity.ok(ApiResponse.success(disputes));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Dispute>> acceptDispute(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> request) {

        String resolvedBy = request != null ? getStringOrDefault(request, "resolvedBy", "SYSTEM") : "SYSTEM";
        Dispute accepted = disputeService.acceptDispute(id, resolvedBy);
        return ResponseEntity.ok(ApiResponse.success(accepted));
    }

    @PostMapping("/{id}/contest")
    public ResponseEntity<ApiResponse<Dispute>> contestDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        String evidenceNotes = getStringOrDefault(request, "evidenceNotes", "");
        String resolvedBy = getStringOrDefault(request, "resolvedBy", "MERCHANT");
        Dispute contested = disputeService.contestDispute(id, evidenceNotes, resolvedBy);
        return ResponseEntity.ok(ApiResponse.success(contested));
    }

    @PostMapping("/{id}/win")
    public ResponseEntity<ApiResponse<Dispute>> winDispute(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> request) {

        String resolvedBy = request != null ? getStringOrDefault(request, "resolvedBy", "SYSTEM") : "SYSTEM";
        Dispute won = disputeService.winDispute(id, resolvedBy);
        return ResponseEntity.ok(ApiResponse.success(won));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Dispute>> closeDispute(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> request) {

        String notes = request != null ? getStringOrDefault(request, "notes", "") : "";
        String resolvedBy = request != null ? getStringOrDefault(request, "resolvedBy", "SYSTEM") : "SYSTEM";
        Dispute closed = disputeService.updateDisputeStatus(id, "CLOSED", notes, resolvedBy);
        return ResponseEntity.ok(ApiResponse.success(closed));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<Dispute>> assignDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        String assignedTo = getStringRequired(request, "assignedTo");
        Dispute assigned = disputeService.assignDispute(id, assignedTo);
        return ResponseEntity.ok(ApiResponse.success(assigned));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<Dispute>>> getOverdueDisputes() {
        List<Dispute> overdue = disputeService.getOverdueDisputes();
        return ResponseEntity.ok(ApiResponse.success(overdue));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "dispute-service"
        );
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    private void validateCreateRequest(Map<String, Object> request) {
        if (!request.containsKey("paymentId") || request.get("paymentId") == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (!request.containsKey("merchantId") || request.get("merchantId") == null) {
            throw new IllegalArgumentException("merchantId is required");
        }
        if (!request.containsKey("amount") || request.get("amount") == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (!request.containsKey("disputeReason") || request.get("disputeReason") == null) {
            throw new IllegalArgumentException("disputeReason is required");
        }
        if (!request.containsKey("disputeType") || request.get("disputeType") == null) {
            throw new IllegalArgumentException("disputeType is required");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getStringRequired(Map<String, Object> map, String key) {
        String value = getString(map, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }
}
