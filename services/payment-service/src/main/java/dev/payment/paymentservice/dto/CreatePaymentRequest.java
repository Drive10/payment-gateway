package dev.payment.paymentservice.dto;

import java.util.UUID;

public class CreatePaymentRequest {

    private UUID orderId;
    private Long amount;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
