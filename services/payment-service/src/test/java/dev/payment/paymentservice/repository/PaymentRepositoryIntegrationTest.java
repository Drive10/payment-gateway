package dev.payment.paymentservice;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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

    @Test
    void shouldSaveAndRetrievePayment() {
        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .merchantId(UUID.randomUUID().toString())
                .paymentMethod("CARD")
                .build();

        Payment saved = entityManager.persistFlushFind(payment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransactionId()).isEqualTo(payment.getTransactionId());
        assertThat(saved.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(saved.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    @Test
    void shouldFindByTransactionId() {
        UUID transactionId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .status(Payment.PaymentStatus.COMPLETED)
                .merchantId(UUID.randomUUID().toString())
                .paymentMethod("UPI")
                .build();

        entityManager.persistFlushFind(payment);

        Optional<Payment> found = paymentRepository.findByTransactionId(transactionId);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldFindByMerchantId() {
        String merchantId = UUID.randomUUID().toString();
        
        Payment payment1 = Payment.builder()
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .merchantId(merchantId)
                .paymentMethod("CARD")
                .build();

        Payment payment2 = Payment.builder()
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .merchantId(merchantId)
                .paymentMethod("CARD")
                .build();

        entityManager.persistFlushFind(payment1);
        entityManager.persistFlushFind(payment2);

        var payments = paymentRepository.findByMerchantId(merchantId);

        assertThat(payments).hasSize(2);
    }

    @Test
    void shouldUpdatePaymentStatus() {
        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .merchantId(UUID.randomUUID().toString())
                .paymentMethod("CARD")
                .build();

        Payment saved = entityManager.persistFlushFind(payment);
        saved.setStatus(Payment.PaymentStatus.COMPLETED);
        entityManager.flush();

        Payment updated = entityManager.find(Payment.class, saved.getId());

        assertThat(updated.getStatus()).isEqualTo(Payment.PaymentStatus.COMPLETED);
    }

    @Test
    void shouldDeletePayment() {
        Payment payment = Payment.builder()
                .transactionId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .merchantId(UUID.randomUUID().toString())
                .paymentMethod("CARD")
                .build();

        Payment saved = entityManager.persistFlushFind(payment);
        Long id = saved.getId();

        entityManager.remove(saved);
        entityManager.flush();

        Payment deleted = entityManager.find(Payment.class, id);

        assertThat(deleted).isNull();
    }
}