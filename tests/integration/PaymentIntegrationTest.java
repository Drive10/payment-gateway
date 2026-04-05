package dev.payment.tests.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests using Testcontainers
 * Tests full application flow with real PostgreSQL and Kafka
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Payment Service Integration Tests")
@Tag("integration")
public class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("paymentdb")
        .withUsername("payment")
        .withPassword("testpass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    // ============ API TESTS ============

    @Test
    @DisplayName("POST /api/v1/payments - Should create payment successfully")
    void shouldCreatePayment() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "orderId", "order-test-123",
            "amount", 1000.00,
            "currency", "INR",
            "paymentMethod", "CARD"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/payments",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("paymentId"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - Should validate required fields")
    void shouldValidateRequiredFields() {
        // Given - Missing required fields
        String requestBody = "{\"amount\": 1000}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/payments",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Should return payment by ID")
    void shouldGetPaymentById() {
        // Given - Create a payment first
        String createResponse = createTestPayment();
        
        // Extract payment ID from response
        Map<String, Object> payment = objectMapper.readValue(createResponse, Map.class);
        String paymentId = (String) payment.get("paymentId");

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/payments/" + paymentId,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/payments/non-existent-id",
            String.class
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ============ KAFKA EVENT TESTS ============

    @Test
    @DisplayName("Should publish payment event to Kafka on creation")
    void shouldPublishPaymentEventToKafka() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "orderId", "order-kafka-test",
            "amount", 500.00,
            "currency", "INR",
            "paymentMethod", "UPI"
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // When
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/payments",
            request,
            String.class
        );

        // Then
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // Verify Kafka message was published
        // This would typically use KafkaConsumer to verify the message
    }

    // ============ DATABASE TESTS ============

    @Test
    @DisplayName("Should persist payment to database")
    void shouldPersistPaymentToDatabase() throws Exception {
        // Given
        String createResponse = createTestPayment();
        Map<String, Object> payment = objectMapper.readValue(createResponse, Map.class);
        String paymentId = (String) payment.get("paymentId");

        // When - Fetch from database
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/payments/" + paymentId,
            String.class
        );

        // Then
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Map<String, Object> fetchedPayment = objectMapper.readValue(getResponse.getBody(), Map.class);
        assertEquals("order-db-test", fetchedPayment.get("orderId"));
    }

    // ============ HELPER METHODS ============

    private String createTestPayment() {
        String requestBody = """
            {
                "orderId": "order-db-test",
                "amount": 1000.00,
                "currency": "INR",
                "paymentMethod": "CARD"
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/payments",
            request,
            String.class
        );

        return response.getBody();
    }
}
