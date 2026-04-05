package dev.payment.orderservice;

import dev.payment.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    @DisplayName("should have all expected statuses")
    void shouldHaveAllExpectedStatuses() {
        assertEquals(5, OrderStatus.values().length);
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("should have valid status values")
    void shouldHaveValidStatusValues(OrderStatus status) {
        assertNotNull(status);
        assertNotNull(status.name());
    }

    @Test
    @DisplayName("PENDING should exist")
    void pendingShouldExist() {
        assertEquals(OrderStatus.PENDING, OrderStatus.valueOf("PENDING"));
    }

    @Test
    @DisplayName("COMPLETED should exist")
    void completedShouldExist() {
        assertEquals(OrderStatus.COMPLETED, OrderStatus.valueOf("COMPLETED"));
    }

    @Test
    @DisplayName("CANCELLED should exist")
    void cancelledShouldExist() {
        assertEquals(OrderStatus.CANCELLED, OrderStatus.valueOf("CANCELLED"));
    }
}
