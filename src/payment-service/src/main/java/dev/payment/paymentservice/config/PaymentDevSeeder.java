package dev.payment.paymentservice;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
public class PaymentDevSeeder implements CommandLineRunner {

    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info("Payments already seeded ({} found)", paymentRepository.count());
            return;
        }

        String merchantId = UUID.nameUUIDFromBytes("merchant@test.com".getBytes()).toString();

        createSamplePayment("ORD-DEMO-001", BigDecimal.valueOf(5000), "INR", PaymentStatus.CAPTURED, merchantId);
        createSamplePayment("ORD-DEMO-002", BigDecimal.valueOf(1200), "USD", PaymentStatus.CAPTURED, merchantId);
        createSamplePayment("ORD-DEMO-003", BigDecimal.valueOf(2500), "INR", PaymentStatus.AUTHORIZED, merchantId);
        createSamplePayment("ORD-DEMO-004", BigDecimal.valueOf(800), "EUR", PaymentStatus.FAILED, merchantId);
        createSamplePayment("ORD-DEMO-005", BigDecimal.valueOf(9999), "INR", PaymentStatus.CREATED, merchantId);

        log.info("Seeded 5 sample payments for merchant: {}", merchantId);
    }

    private void createSamplePayment(String orderId, BigDecimal amount, String currency, PaymentStatus status, String merchantId) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .status(status)
                .merchantId(merchantId)
                .paymentMethod("CARD")
                .createdAt(Instant.now().minus(java.time.Duration.ofHours((long)(Math.random() * 48))))
                .updatedAt(Instant.now())
                .simulated(false)
                .platformFee(BigDecimal.ZERO)
                .gatewayFee(BigDecimal.ZERO)
                .refundAmount(BigDecimal.ZERO)
                .transactionMode("TEST")
                .build();

        if (status == PaymentStatus.CAPTURED) {
            payment.setCapturedAt(Instant.now().minus(java.time.Duration.ofMinutes((long)(Math.random() * 60))));
            payment.setProviderReference("PROV_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (status == PaymentStatus.FAILED) {
            payment.setFailureReason("Card declined by issuer");
        }

        paymentRepository.save(payment);
        log.debug("  Created payment {}: {} {} [{}]", orderId, amount, currency, status);
    }
}
