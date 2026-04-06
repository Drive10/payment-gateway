package dev.payment.orderservice;

import dev.payment.orderservice.Order;
import dev.payment.orderservice.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setUserId(UUID.randomUUID());
        order.setAmount(new BigDecimal("100.00"));
        order.setCurrency("INR");
        order.setStatus(OrderStatus.PENDING);
        order.setDescription("Test order");
    }

    @Nested
    @DisplayName("Order Creation")
    class OrderCreation {

        @Test
        @DisplayName("should create order with all fields")
        void shouldCreateOrderWithAllFields() {
            assertNotNull(order);
            assertNotNull(order.getUserId());
            assertEquals(new BigDecimal("100.00"), order.getAmount());
            assertEquals("INR", order.getCurrency());
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals("Test order", order.getDescription());
        }

        @Test
        @DisplayName("should allow setting external reference")
        void shouldAllowSettingExternalReference() {
            order.setExternalReference("EXT-12345");
            assertEquals("EXT-12345", order.getExternalReference());
        }

        @Test
        @DisplayName("should allow setting customer email")
        void shouldAllowSettingCustomerEmail() {
            order.setCustomerEmail("customer@example.com");
            assertEquals("customer@example.com", order.getCustomerEmail());
        }

        @Test
        @DisplayName("should allow setting merchant id")
        void shouldAllowSettingMerchantId() {
            order.setMerchantId("merchant-001");
            assertEquals("merchant-001", order.getMerchantId());
        }
    }

    @Nested
    @DisplayName("Order Status Transitions")
    class OrderStatusTransitions {

        @Test
        @DisplayName("should allow status change from PENDING to PAID")
        void shouldAllowStatusChangeFromPendingToPaid() {
            order.setStatus(OrderStatus.PAID);
            assertEquals(OrderStatus.PAID, order.getStatus());
        }

        @Test
        @DisplayName("should allow status change from PAID to REFUNDED")
        void shouldAllowStatusChangeFromPaidToRefunded() {
            order.setStatus(OrderStatus.PAID);
            order.setStatus(OrderStatus.REFUNDED);
            assertEquals(OrderStatus.REFUNDED, order.getStatus());
        }

        @Test
        @DisplayName("should allow cancellation")
        void shouldAllowCancellation() {
            order.setStatus(OrderStatus.CANCELLED);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
    }

    @Nested
    @DisplayName("Entity Auditing")
    class EntityAuditing {

        @Test
        @DisplayName("should have null id initially")
        void shouldHaveNullIdInitially() {
            assertNull(order.getId());
        }

        @Test
        @DisplayName("should have null timestamps initially")
        void shouldHaveNullTimestampsInitially() {
            assertNull(order.getCreatedAt());
            assertNull(order.getUpdatedAt());
        }
    }
}
