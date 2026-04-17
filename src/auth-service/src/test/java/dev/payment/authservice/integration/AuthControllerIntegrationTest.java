package dev.payment.authservice.integration;

import dev.payment.authservice.dto.LoginRequest;
import dev.payment.authservice.dto.RegisterRequest;
import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.User;
import dev.payment.authservice.repository.RoleRepository;
import dev.payment.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
            
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }
    }

    @Test
    void register_ValidRequest_ReturnsCreatedAndTokens() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register",
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("accessToken"));
        assertTrue(response.getBody().contains("refreshToken"));
    }

    @Test
    void register_DuplicateEmail_ReturnsBadRequest() {
        RegisterRequest request1 = new RegisterRequest(
                "duplicate@example.com",
                "password123",
                "John",
                "Doe",
                "USER"
        );
        
        restTemplate.postForEntity("/auth/register", request1, String.class);

        RegisterRequest request2 = new RegisterRequest(
                "duplicate@example.com",
                "password456",
                "Jane",
                "Doe",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register",
                request2,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_ValidCredentials_ReturnsTokens() {
        RegisterRequest registerRequest = new RegisterRequest(
                "login-test@example.com",
                "password123",
                "Test",
                "User",
                "USER"
        );
        restTemplate.postForEntity("/auth/register", registerRequest, String.class);

        LoginRequest loginRequest = new LoginRequest("login-test@example.com", "password123");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("accessToken"));
    }

    @Test
    void login_InvalidPassword_ReturnsUnauthorized() {
        RegisterRequest registerRequest = new RegisterRequest(
                "wrong-pass@example.com",
                "correctPassword",
                "Test",
                "User",
                "USER"
        );
        restTemplate.postForEntity("/auth/register", registerRequest, String.class);

        LoginRequest loginRequest = new LoginRequest("wrong-pass@example.com", "wrongPassword");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void login_NonExistingUser_ReturnsUnauthorized() {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login",
                loginRequest,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokens() {
        RegisterRequest registerRequest = new RegisterRequest(
                "refresh-test@example.com",
                "password123",
                "Test",
                "User",
                "USER"
        );
        
        String registerResponse = restTemplate.postForEntity(
                "/auth/register",
                registerRequest,
                String.class
        ).getBody();

        String refreshToken = extractRefreshToken(registerResponse);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/refresh",
                java.util.Map.of("refreshToken", refreshToken),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("accessToken"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "invalid-email",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "123",
                "John",
                "Doe",
                "USER"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/register",
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private String extractRefreshToken(String responseBody) {
        String key = "\"refreshToken\":\"";
        int start = responseBody.indexOf(key) + key.length();
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }
}