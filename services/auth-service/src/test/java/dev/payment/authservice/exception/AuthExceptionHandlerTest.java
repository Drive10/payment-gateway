package dev.payment.authservice.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthExceptionHandlerTest {

    @InjectMocks
    private AuthExceptionHandler handler;

    @Test
    void handleAuthException_ReturnsBadRequest() {
        AuthException ex = new AuthException("Email already exists", "EMAIL_EXISTS");

        ResponseEntity<Map<String, Object>> response = handler.handleAuthException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void handleAuthException_ReturnsCorrectErrorCode() {
        AuthException ex = new AuthException("User not found", "USER_NOT_FOUND");

        ResponseEntity<Map<String, Object>> response = handler.handleAuthException(ex);

        Map<String, Object> body = response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("USER_NOT_FOUND", error.get("code"));
    }

    @Test
    void handleBadCredentials_ReturnsUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("INVALID_CREDENTIALS", error.get("code"));
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("INTERNAL_ERROR", error.get("code"));
    }
}