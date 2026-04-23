package dev.payment.combinedservice.payment.service;

import dev.payment.combinedservice.payment.domain.Payment;
import dev.payment.combinedservice.payment.domain.enums.TransactionMode;
import dev.payment.combinedservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.combinedservice.payment.exception.ApiException;
import org.springframework.http.HttpStatus;
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
            throw ApiException.badRequest("VALIDATION_ERROR", String.join(", ", errors));
        }
    }

    public void validatePaymentForCapture(Payment payment) {
        if (payment == null) {
            throw ApiException.notFound("PAYMENT_NOT_FOUND", "Payment is required");
        }
        
        if (payment.getStatus() == null) {
            throw ApiException.badRequest("INVALID_STATE", "Payment status is required");
        }
    }

    private boolean isValidProvider(String provider) {
        return provider == null || 
               provider.equals("RAZORPAY") || 
               provider.equals("STRIPE") ||
               provider.equals("RAZORPAY_SIMULATOR");
    }
}