package dev.payment.merchantbackend.dto;

import dev.payment.common.dto.OrderResponse;
import dev.payment.common.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

class CreatePaymentResponseTest {

    @Test
    void shouldCreatePaymentResponse() {
        OrderResponse order = new OrderResponse();
        order.setId(UUID.randomUUID());
        order.setOrderReference("ORDER-123");
        order.setAmount(BigDecimal.valueOf(1000));
        order.setCurrency("USD");
        order.setStatus("PENDING");

        PaymentResponse payment = new PaymentResponse();
        payment.setId(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setStatus("PENDING");
        payment.setCheckoutUrl("http://checkout.example.com/pay/123");

        CreatePaymentResponse response = CreatePaymentResponse.from(order, payment);

        assertNotNull(response.getOrder());
        assertNotNull(response.getPayment());
        assertEquals("http://checkout.example.com/pay/123", response.getCheckoutUrl());
    }

    @Test
    void shouldHandleNullPayment() {
        OrderResponse order = new OrderResponse();
        order.setId(UUID.randomUUID());

        CreatePaymentResponse response = CreatePaymentResponse.from(order, null);

        assertNotNull(response.getOrder());
        assertNull(response.getPayment());
        assertNull(response.getCheckoutUrl());
    }
}