package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.dto.request.CreatePaymentRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentValidator {

    public List<String> validate(CreatePaymentRequest request) {
        List<String> errors = new ArrayList<>();
        
        if (request.orderId() == null) {
            errors.add("Order ID is required");
        }
        
        if (request.merchantId() == null) {
            errors.add("Merchant ID is required");
        }
        
        if (request.method() == null) {
            errors.add("Payment method is required");
        }
        
        if (request.provider() != null && !isValidProvider(request.provider())) {
            errors.add("Invalid payment provider");
        }
        
        return errors;
    }

    public void validateOrThrow(CreatePaymentRequest request) {
        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void validatePaymentForCapture(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        
        if (payment.getStatus() == null) {
            throw new IllegalArgumentException("Payment status is required");
        }
    }

    private boolean isValidProvider(String provider) {
        return provider == null || 
               provider.equals("RAZORPAY") || 
               provider.equals("STRIPE") ||
               provider.equals("RAZORPAY_SIMULATOR");
    }
}