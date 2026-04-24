package dev.payment.merchantbackend.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.merchantbackend.dto.CreatePaymentResponse;
import dev.payment.merchantbackend.dto.FrontendPaymentRequest;
import dev.payment.merchantbackend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    private final PaymentService paymentService;

    public MerchantController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody FrontendPaymentRequest request) {
        
        String customerEmail = getCurrentUserEmail();
        
        CreatePaymentResponse response = paymentService.createPayment(
            request.productId(),
            customerEmail
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        return "customer@example.com";
    }
}