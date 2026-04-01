package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.domain.enums.PaymentMethod;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("password");
        entityManager.persist(testUser);

        testOrder = new Order();
        testOrder.setOrderReference("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        testOrder.setExternalReference("EXT-" + UUID.randomUUID().toString().substring(0, 8));
        testOrder.setAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("USD");
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setDescription("Test order");
        testOrder.setUser(testUser);
        entityManager.persist(testOrder);
        entityManager.flush();
    }

    @Test
    void shouldSaveAndRetrievePayment() {
        Payment payment = createPayment("prov-order-" + UUID.randomUUID(), "prov-pay-" + UUID.randomUUID());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTransactionMode(TransactionMode.TEST);

        Payment saved = paymentRepository.save(payment);
        entityManager.flush();

        Optional<Payment> found = paymentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void shouldFindByProviderOrderId() {
        String providerOrderId = "prov-order-" + UUID.randomUUID();
        
        Payment payment = createPayment(providerOrderId, "prov-pay-" + UUID.randomUUID());
        paymentRepository.save(payment);
        entityManager.flush();

        Optional<Payment> found = paymentRepository.findByProviderOrderId(providerOrderId);

        assertThat(found).isPresent();
        assertThat(found.get().getProviderOrderId()).isEqualTo(providerOrderId);
    }

    @Test
    void shouldFindByProviderPaymentId() {
        String providerPaymentId = "prov-pay-" + UUID.randomUUID();
        
        Payment payment = createPayment("prov-order-" + UUID.randomUUID(), providerPaymentId);
        paymentRepository.save(payment);
        entityManager.flush();

        Optional<Payment> found = paymentRepository.findByProviderPaymentId(providerPaymentId);

        assertThat(found).isPresent();
        assertThat(found.get().getProviderPaymentId()).isEqualTo(providerPaymentId);
    }

    @Test
    void shouldFindByIdempotencyKey() {
        String idempotencyKey = "idem-" + UUID.randomUUID();
        
        Payment payment = createPayment("prov-order-" + UUID.randomUUID(), "prov-pay-" + UUID.randomUUID());
        payment.setIdempotencyKey(idempotencyKey);
        paymentRepository.save(payment);
        entityManager.flush();

        Optional<Payment> found = paymentRepository.findByIdempotencyKey(idempotencyKey);

        assertThat(found).isPresent();
    }

    @Test
    void shouldFindPaymentsByOrderUser() {
        Payment payment1 = createPayment("prov-order-1", "prov-pay-1");
        payment1.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment1);

        Payment payment2 = createPayment("prov-order-2", "prov-pay-2");
        payment2.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment2);
        entityManager.flush();

        Page<Payment> payments = paymentRepository.findByOrderUser(testUser, PageRequest.of(0, 10));

        assertThat(payments.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFindPaymentsByOrderUserAndStatus() {
        Payment payment1 = createPayment("prov-order-1", "prov-pay-1");
        payment1.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment1);

        Payment payment2 = createPayment("prov-order-2", "prov-pay-2");
        payment2.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment2);
        entityManager.flush();

        Page<Payment> createdPayments = paymentRepository.findByOrderUserAndStatus(
                testUser, PaymentStatus.CREATED, PageRequest.of(0, 10));

        assertThat(createdPayments.getTotalElements()).isEqualTo(1);
        assertThat(createdPayments.getContent().get(0).getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void shouldFindPaymentsByStatus() {
        Payment payment1 = createPayment("prov-order-1", "prov-pay-1");
        payment1.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment1);

        Payment payment2 = createPayment("prov-order-2", "prov-pay-2");
        payment2.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment2);

        Payment payment3 = createPayment("prov-order-3", "prov-pay-3");
        payment3.setStatus(PaymentStatus.CREATED);
        paymentRepository.save(payment3);
        entityManager.flush();

        Page<Payment> capturedPayments = paymentRepository.findByStatus(
                PaymentStatus.CAPTURED, PageRequest.of(0, 10));

        assertThat(capturedPayments.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFindPaymentsByStatusInAndUpdatedAtAfter() {
        Payment payment1 = createPayment("prov-order-1", "prov-pay-1");
        payment1.setStatus(PaymentStatus.CREATED);
        entityManager.persist(payment1);

        Payment payment2 = createPayment("prov-order-2", "prov-pay-2");
        payment2.setStatus(PaymentStatus.PROCESSING);
        entityManager.persist(payment2);

        Payment payment3 = createPayment("prov-order-3", "prov-pay-3");
        payment3.setStatus(PaymentStatus.CAPTURED);
        entityManager.persist(payment3);
        entityManager.flush();

        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        List<Payment> pendingPayments = paymentRepository.findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(
                List.of(PaymentStatus.CREATED, PaymentStatus.PROCESSING),
                oneHourAgo,
                PageRequest.of(0, 10));

        assertThat(pendingPayments).hasSize(2);
    }

    @Test
    void shouldUpdatePaymentStatus() {
        Payment payment = createPayment("prov-order-update", "prov-pay-update");
        payment.setStatus(PaymentStatus.CREATED);
        Payment saved = paymentRepository.save(payment);
        entityManager.flush();

        saved.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Payment updated = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void shouldDeletePayment() {
        Payment payment = createPayment("prov-order-delete", "prov-pay-delete");
        Payment saved = paymentRepository.save(payment);
        UUID id = saved.getId();
        entityManager.flush();

        paymentRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> deleted = paymentRepository.findById(id);
        assertThat(deleted).isEmpty();
    }

    private Payment createPayment(String providerOrderId, String providerPaymentId) {
        Payment payment = new Payment();
        payment.setOrder(testOrder);
        payment.setProviderOrderId(providerOrderId);
        payment.setProviderPaymentId(providerPaymentId);
        payment.setIdempotencyKey("idem-" + UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setProvider("STRIPE");
        payment.setMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTransactionMode(TransactionMode.TEST);
        payment.setSimulated(false);
        payment.setCheckoutUrl("https://checkout.example.com");
        payment.setMerchantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return payment;
    }
}
