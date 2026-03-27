package dev.payment.paymentservice.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.containers.PostgreSQLContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataJpaTest
public class PaymentServiceIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Configuration
    static class TestConfig {
        @Bean
        public PostgreSQLContainer<?> postgresql() {
            return new PostgreSQLContainer<>("postgres:latest")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresql().getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresql().getUsername());
        registry.add("spring.datasource.password", () -> postgresql().getPassword());
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void testCreatePayment() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);

        orderRepository.save(order);

        Payment result = paymentService.createPayment(orderId, "providerOrderId", BigDecimal.TEN);

        assertNotNull(result);
        assertEquals(order, result.getOrder());
    }

    @Test
    void testCreatePaymentOrderNotFound() {
        UUID orderId = UUID.randomUUID();

        assertThrows(ApiException.class, () -> paymentService.createPayment(orderId, "providerOrderId", BigDecimal.TEN));
    }
}
