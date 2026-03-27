package dev.payment.paymentservice.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
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
public class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

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
    }

    @Test
    void testFindByIdAndUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        Order order = new Order();
        order.setId(id);
        order.setUser(user);

        orderRepository.save(order);

        Optional<Order> result = orderRepository.findByIdAndUser(id, user);

        assertTrue(result.isPresent());
        assertEquals(order, result.get());
    }

    @Test
    void testFindByIdAndUserNotFound() {
        UUID id = UUID.randomUUID();
        User user = new User();

        Optional<Order> result = orderRepository.findByIdAndUser(id, user);

        assertFalse(result.isPresent());
    }
}
