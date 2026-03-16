package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentStatus;
import dev.payment.paymentservice.dto.CreatePaymentRequest;
import dev.payment.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(CreatePaymentRequest request) {

        Payment payment = new Payment();

        payment.setId(UUID.randomUUID());
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency("INR");
        payment.setProvider("LOCAL_SIMULATOR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }
}