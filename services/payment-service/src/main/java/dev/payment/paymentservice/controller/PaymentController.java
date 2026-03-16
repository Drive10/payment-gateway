package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.dto.CreatePaymentRequest;
import dev.payment.paymentservice.dto.PaymentResponse;
import dev.payment.paymentservice.service.PaymentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(@RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.createPayment(request);

        return new PaymentResponse(
                payment.getId(),
                payment.getStatus().name()
        );
    }
}