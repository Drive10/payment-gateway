package dev.payment.paymentservice.repository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrderRepositoryTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindByIdAndUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        Optional<Order> expectedOrder = Optional.of(new Order());

        when(orderRepository.findByIdAndUser(id, user)).thenReturn(expectedOrder);

        Optional<Order> result = orderService.findByIdAndUser(id, user);

        assertTrue(result.isPresent());
        assertEquals(expectedOrder.get(), result.get());
    }

    @Test
    void testFindByIdAndUserNotFound() {
        UUID id = UUID.randomUUID();
        User user = new User();

        when(orderRepository.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findByIdAndUser(id, user);

        assertFalse(result.isPresent());
    }
}
