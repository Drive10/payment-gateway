package dev.payment.merchantbackend.dto;

import dev.payment.common.dto.OrderResponse;
import dev.payment.common.dto.PaymentResponse;

public class CreatePaymentResponse {
    private OrderResponse order;
    private PaymentResponse payment;
    private String checkoutUrl;

    public CreatePaymentResponse() {}

    public CreatePaymentResponse(OrderResponse order, PaymentResponse payment, String checkoutUrl) {
        this.order = order;
        this.payment = payment;
        this.checkoutUrl = checkoutUrl;
    }

    public static CreatePaymentResponse from(OrderResponse order, PaymentResponse payment) {
        return new CreatePaymentResponse(
            order,
            payment,
            payment != null ? payment.getCheckoutUrl() : null
        );
    }

    public OrderResponse getOrder() { return order; }
    public void setOrder(OrderResponse order) { this.order = order; }
    public PaymentResponse getPayment() { return payment; }
    public void setPayment(PaymentResponse payment) { this.payment = payment; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
}