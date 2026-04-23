package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.payment.exception.ApiException;
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

        if (request.method() == PaymentMethod.UPI
                && request.provider() != null
                && !request.provider().toUpperCase().contains("RAZORPAY")) {
            errors.add("UPI flow currently supports RAZORPAY provider only");
        }

        // Basic fraud/risk heuristics for test mode simulation.
        if (request.notes() != null) {
            String notes = request.notes().toLowerCase();
            if (notes.contains("stolen") || notes.contains("fraud") || notes.contains("blacklisted")) {
                errors.add("Payment blocked by basic fraud policy");
            }
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
               provider.equals("PAYPAL") ||
               provider.equals("STRIPE") ||
               provider.equals("RAZORPAY_SIMULATOR") ||
               provider.equals("PAYPAL_SIMULATOR") ||
               provider.equals("STRIPE_SIMULATOR");
    }
}
