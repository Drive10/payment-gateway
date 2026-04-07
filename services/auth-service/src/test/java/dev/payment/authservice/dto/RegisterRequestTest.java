package dev.payment.authservice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTI1Ni13aGljaC1yZXF1aXJlcy1hdC1sZWFzdC0zMi1jaGFyYWN0ZXJz",
    "jwt.expiration=86400000",
    "jwt.refresh-expiration=604800000"
})
class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldAcceptValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no violations for valid request");
    }

    @Test
    void shouldRejectInvalidEmail() {
        RegisterRequest request = new RegisterRequest(
                "invalid-email",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Expected violations for invalid email");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Invalid email format")),
                "Expected email format violation");
    }

    @Test
    void shouldRejectShortPassword() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "123",
                "John",
                "Doe",
                "USER"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Expected violations for short password");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must be between 8 and 100 characters")),
                "Expected password length violation");
    }

    @Test
    void shouldRejectMissingFirstName() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "",
                "Doe",
                "USER"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Expected violations for missing first name");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("First name is required")),
                "Expected first name required violation");
    }
}