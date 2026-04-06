package dev.payflow.gateway.controller;

import dev.payflow.gateway.document.PaymentLink;
import dev.payflow.gateway.document.Transaction;
import dev.payflow.gateway.dto.PaymentLinkResponse;
import dev.payflow.gateway.service.PaymentLinkService;
import dev.payflow.gateway.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PublicPaymentController {

    private final PaymentLinkService paymentLinkService;
    private final TransactionService transactionService;

    public PublicPaymentController(PaymentLinkService paymentLinkService, TransactionService transactionService) {
        this.paymentLinkService = paymentLinkService;
        this.transactionService = transactionService;
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<PaymentLinkResponse> getPaymentLink(@PathVariable String uuid) {
        if (!paymentLinkService.validate(uuid)) {
            return ResponseEntity.notFound().build();
        }
        PaymentLink link = paymentLinkService.findByUuid(uuid);
        return ResponseEntity.ok(toResponse(link));
    }

    @PostMapping("/{uuid}")
    public ResponseEntity<Map<String, Object>> processPayment(
            @PathVariable String uuid,
            @RequestParam String paymentMethod,
            @RequestParam(defaultValue = "true") boolean testMode) {
        
        if (!paymentLinkService.validate(uuid)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired payment link"));
        }
        
        Transaction tx = transactionService.processPayment(uuid, paymentMethod, testMode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", tx.getTransactionId());
        response.put("status", tx.getStatus().name());
        response.put("message", tx.getGatewayResponse());
        
        return ResponseEntity.ok(response);
    }

    private PaymentLinkResponse toResponse(PaymentLink link) {
        return new PaymentLinkResponse(
            link.getUuid(),
            link.getOrderNumber(),
            link.getDescription(),
            link.getAmount(),
            link.getCurrency(),
            link.getStatus().name(),
            link.getExpiresAt().toString(),
            "/pay/" + link.getUuid()
        );
    }
}
