package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.entity.Outbox;
import dev.payment.paymentservice.entity.LedgerEntry;
import dev.payment.paymentservice.entity.LedgerEntry.AccountType;
import dev.payment.paymentservice.entity.LedgerEntry.EntryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testpayflow")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        Payment payment = Payment.builder()
                .orderId("order-test-1")
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .merchantId("merchant-test-1")
                .correlationId(UUID.randomUUID().toString())
                .clientSecret("pi_test_secret")
                .build();
        savedPayment = paymentRepository.save(payment);
    }

    @Test
    void findByOrderId_ShouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findByOrderId("order-test-1");
        assertTrue(found.isPresent());
        assertEquals("order-test-1", found.get().getOrderId());
        assertEquals(new BigDecimal("5000"), found.get().getAmount());
    }

    @Test
    void findByOrderId_WithNonExistentOrder_ShouldReturnEmpty() {
        Optional<Payment> found = paymentRepository.findByOrderId("non-existent");
        assertTrue(found.isEmpty());
    }

    @Test
    void findByCorrelationId_ShouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findByCorrelationId(savedPayment.getCorrelationId());
        assertTrue(found.isPresent());
        assertEquals(savedPayment.getId(), found.get().getId());
    }

    @Test
    void existsByCorrelationId_ShouldReturnTrue() {
        assertTrue(paymentRepository.existsByCorrelationId(savedPayment.getCorrelationId()));
    }

    @Test
    void existsByCorrelationId_WithNonExistent_ShouldReturnFalse() {
        assertFalse(paymentRepository.existsByCorrelationId("non-existent"));
    }

    @Test
    void findByMerchantId_ShouldReturnPayments() {
        Payment payment2 = Payment.builder()
                .orderId("order-test-2")
                .amount(new BigDecimal("3000"))
                .currency("USD")
                .status(PaymentStatus.CREATED)
                .merchantId("merchant-test-1")
                .correlationId(UUID.randomUUID().toString())
                .clientSecret("pi_test_secret_2")
                .build();
        paymentRepository.save(payment2);

        List<Payment> payments = paymentRepository.findByMerchantId("merchant-test-1");
        assertEquals(2, payments.size());
    }

    @Test
    void findByStatusIn_ShouldReturnPaymentsWithMatchingStatus() {
        Payment captured = Payment.builder()
                .orderId("order-captured")
                .amount(new BigDecimal("1000"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .merchantId("merchant-test-1")
                .correlationId(UUID.randomUUID().toString())
                .clientSecret("pi_captured")
                .build();
        paymentRepository.save(captured);

        List<Payment> results = paymentRepository.findByStatusIn(List.of(PaymentStatus.CAPTURED));
        assertEquals(1, results.size());
        assertEquals(PaymentStatus.CAPTURED, results.get(0).getStatus());
    }

    @Test
    void findByIdWithLock_ShouldReturnPayment() {
        Optional<Payment> found = paymentRepository.findByIdWithLock(savedPayment.getId());
        assertTrue(found.isPresent());
        assertEquals(savedPayment.getId(), found.get().getId());
    }

    @Test
    void savePayment_ShouldSetTimestamps() {
        assertNotNull(savedPayment.getCreatedAt());
        assertNotNull(savedPayment.getUpdatedAt());
    }

    @Test
    void paymentDefaults_ShouldBeSet() {
        assertEquals(BigDecimal.ZERO, savedPayment.getRefundAmount());
        assertEquals(BigDecimal.ZERO, savedPayment.getPlatformFee());
        assertEquals(BigDecimal.ZERO, savedPayment.getGatewayFee());
        assertEquals(PaymentStatus.CREATED, savedPayment.getStatus());
    }

    @Test
    void outbox_SaveAndFindUnprocessedEvents() {
        Outbox event = Outbox.builder()
                .aggregateId(savedPayment.getId().toString())
                .eventType("PAYMENT_CREATED")
                .payload("{\"paymentId\":\"" + savedPayment.getId() + "\"}")
                .createdAt(Instant.now())
                .build();
        outboxRepository.save(event);

        List<Outbox> unprocessed = outboxRepository.findUnprocessedEvents();
        assertFalse(unprocessed.isEmpty());
        assertTrue(unprocessed.stream().anyMatch(e -> e.getAggregateId().equals(savedPayment.getId().toString())));
    }

    @Test
    void outbox_ClaimEvent_ShouldMarkProcessed() {
        Outbox event = Outbox.builder()
                .aggregateId(savedPayment.getId().toString())
                .eventType("PAYMENT_CREATED")
                .payload("{\"paymentId\":\"" + savedPayment.getId() + "\"}")
                .createdAt(Instant.now())
                .build();
        event = outboxRepository.save(event);

        int updated = outboxRepository.claimEvent(event.getId());
        assertEquals(1, updated);

        Outbox claimed = outboxRepository.findById(event.getId()).orElseThrow();
        assertNotNull(claimed.getProcessedAt());
    }

    @Test
    void ledgerEntry_SaveAndFindByMerchant() {
        LedgerEntry entry = LedgerEntry.builder()
                .accountId("merchant-test-1")
                .accountType(AccountType.MERCHANT_RECEIVABLE)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .reference("test-ref-1")
                .paymentId(savedPayment.getId().toString())
                .merchantId("merchant-test-1")
                .postedAt(Instant.now())
                .build();
        ledgerEntryRepository.save(entry);

        List<LedgerEntry> entries = ledgerEntryRepository.findByMerchantId("merchant-test-1");
        assertEquals(1, entries.size());
        assertEquals(new BigDecimal("5000"), entries.get(0).getAmount());
    }

    @Test
    void ledgerEntry_ExistsByReference_ShouldReturnTrue() {
        LedgerEntry entry = LedgerEntry.builder()
                .accountId("merchant-test-1")
                .accountType(AccountType.MERCHANT_RECEIVABLE)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .reference("unique-ref-123")
                .paymentId(savedPayment.getId().toString())
                .merchantId("merchant-test-1")
                .build();
        ledgerEntryRepository.save(entry);

        assertTrue(ledgerEntryRepository.existsByReference("unique-ref-123"));
        assertFalse(ledgerEntryRepository.existsByReference("non-existent-ref"));
    }

    @Test
    void ledgerEntry_FindByPaymentId_ShouldReturnEntries() {
        LedgerEntry entry1 = LedgerEntry.builder()
                .accountId("merchant-test-1")
                .accountType(AccountType.MERCHANT_RECEIVABLE)
                .entryType(EntryType.CREDIT)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .reference("ref-1")
                .paymentId(savedPayment.getId().toString())
                .merchantId("merchant-test-1")
                .build();
        LedgerEntry entry2 = LedgerEntry.builder()
                .accountId("merchant-test-1")
                .accountType(AccountType.CUSTOMER_ESCROW)
                .entryType(EntryType.DEBIT)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .reference("ref-2")
                .paymentId(savedPayment.getId().toString())
                .merchantId("merchant-test-1")
                .build();
        ledgerEntryRepository.save(entry1);
        ledgerEntryRepository.save(entry2);

        List<LedgerEntry> entries = ledgerEntryRepository.findByPaymentId(savedPayment.getId().toString());
        assertEquals(2, entries.size());
    }

    @Test
    void paymentStatusTransition_ShouldUpdateStatus() {
        savedPayment.setStatus(PaymentStatus.AUTHORIZED);
        paymentRepository.save(savedPayment);

        Payment updated = paymentRepository.findById(savedPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.AUTHORIZED, updated.getStatus());
    }

    @Test
    void findByIdempotencyKey_ShouldReturnPayment() {
        savedPayment.setIdempotencyKey("idem-key-123");
        paymentRepository.save(savedPayment);

        Optional<Payment> found = paymentRepository.findByIdempotencyKey("idem-key-123");
        assertTrue(found.isPresent());
    }
}
