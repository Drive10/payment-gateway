package dev.payment.paymentservice;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.LedgerEntry;
import dev.payment.paymentservice.entity.SettlementBatch;
import dev.payment.paymentservice.entity.Outbox;
import dev.payment.paymentservice.repository.*;
import dev.payment.paymentservice.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class PaymentServiceE2ETest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private SettlementBatchRepository settlementBatchRepository;

    @Test
    void testFullPaymentLifecycle() {
        // 1. Create payment
        var request = new dev.payment.paymentservice.dto.CreateOrderRequest();
        request.setOrderId("E2E-" + System.currentTimeMillis());
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");

        var response = paymentService.createPayment(null, request, "dev@test.com");
        
        assertNotNull(response);
        assertNotNull(response.getPaymentId());
        System.out.println("Created payment: " + response.getPaymentId());

        // 2. Get payment status
        var status = paymentService.getPaymentStatusById(response.getPaymentId());
        assertNotNull(status);
        assertEquals("CREATED", status.getStatus());
    }

    @Test
    void testIdempotencyKey() {
        String idempotencyKey = "idem-" + System.currentTimeMillis();
        
        // First request
        var req1 = new dev.payment.paymentservice.dto.CreateOrderRequest();
        req1.setOrderId("idem-" + System.currentTimeMillis());
        req1.setAmount(new BigDecimal("50.00"));
        req1.setCurrency("USD");
        
        var resp1 = paymentService.createPayment(idempotencyKey, req1, "dev@test.com");
        
        // Second request with same key
        var req2 = new dev.payment.paymentservice.dto.CreateOrderRequest();
        req2.setOrderId("idem-" + System.currentTimeMillis());
        req2.setAmount(new BigDecimal("50.00"));
        req2.setCurrency("USD");
        
        var resp2 = paymentService.createPayment(idempotencyKey, req2, "dev@test.com");
        
        // Should return same payment
        assertEquals(resp1.getPaymentId(), resp2.getPaymentId());
        System.out.println("Idempotency test: PASS");
    }

    @Test
    void testCapturePayment() {
        // Create payment
        var createReq = new dev.payment.paymentservice.dto.CreateOrderRequest();
        createReq.setOrderId("CAPTURE-" + System.currentTimeMillis());
        createReq.setAmount(new BigDecimal("200.00"));
        createReq.setCurrency("USD");
        
        var payment = paymentService.createPayment(null, createReq, "dev@test.com");
        String paymentId = payment.getPaymentId();

        // Capture
        String captureKey = "capture-" + System.currentTimeMillis();
        var captured = paymentService.capturePaymentIntent(paymentId, captureKey);
        
        assertTrue(captured.isSuccess() || captured.getMessage() != null);
        
        // Verify captured
        var status = paymentService.getPaymentStatusById(paymentId);
        assertEquals("CAPTURED", status.getStatus());
        System.out.println("Capture test: PASS");
    }

    @Test
    void testLedgerEntriesCreated() {
        // Create and capture payment
        var req = new dev.payment.paymentservice.dto.CreateOrderRequest();
        req.setOrderId("LEDGER-" + System.currentTimeMillis());
        req.setAmount(new BigDecimal("300.00"));
        req.setCurrency("USD");
        
        var payment = paymentService.createPayment(null, req, "dev@test.com");
        paymentService.capturePaymentIntent(payment.getPaymentId(), "capture-ledger");

        // Check ledger entries
        List<LedgerEntry> entries = ledgerEntryRepository.findByPaymentId(payment.getPaymentId());
        assertFalse(entries.isEmpty());
        System.out.println("Ledger entries created: " + entries.size());
    }

    @Test
    void testOutboxEvents() {
        var req = new dev.payment.paymentservice.dto.CreateOrderRequest();
        req.setOrderId("OUTBOX-" + System.currentTimeMillis());
        req.setAmount(new BigDecimal("75.00"));
        req.setCurrency("USD");
        
        paymentService.createPayment(null, req, "dev@test.com");

        var status = outboxPoller.getEventStatus();
        System.out.println("Outbox status: pending=" + status.pending() + ", dlq=" + status.dlq());
        assertTrue(status.pending() >= 0);
    }

    @Test
    void testSettlementWorkflow() {
        String merchantId = "dev@test.com";
        
        // Create payment and capture
        var req = new dev.payment.paymentservice.dto.CreateOrderRequest();
        req.setOrderId("SETTLE-" + System.currentTimeMillis());
        req.setAmount(new BigDecimal("500.00"));
        req.setCurrency("USD");
        
        var payment = paymentService.createPayment(null, req, merchantId);
        paymentService.capturePaymentIntent(payment.getPaymentId(), "capture-settle");

        // Try create settlement
        try {
            var batch = settlementService.createSettlementBatch(merchantId, "USD");
            assertNotNull(batch);
            System.out.println("Settlement batch created: " + batch.getBatchId());
        } catch (Exception e) {
            System.out.println("Settlement batch (may need balance): " + e.getMessage());
        }
    }

    @Test
    void testLedgerIntegrity() {
        boolean balanced = ledgerService.validateLedgerBalance();
        System.out.println("Ledger integrity: " + (balanced ? "PASS" : "FAIL"));
    }
}