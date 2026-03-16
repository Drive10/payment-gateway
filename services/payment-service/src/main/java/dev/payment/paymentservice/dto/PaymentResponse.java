package dev.payment.paymentservice.dto;


import java.util.UUID;

public class PaymentResponse {

    private UUID paymentId;
    private String status;

    public PaymentResponse(UUID paymentId, String status) {
        this.paymentId = paymentId;
        this.status = status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getStatus() {
        return status;
    }
}