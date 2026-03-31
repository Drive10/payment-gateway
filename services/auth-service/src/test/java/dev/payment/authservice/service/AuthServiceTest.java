package dev.payment.authservice.service;

import dev.payment.authservice.dto.AuthResponse;
import dev.payment.authservice.dto.LoginRequest;
import dev.payment.authservice.dto.RegisterRequest;
import dev.payment.authservice.dto.UserResponse;
import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.User;
import dev.payment.authservice.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, jwtService, passwordEncoder);
    }

    private User createTestUser() {
        Role role = new Role(1L, "USER", "Regular user");
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "test@example.com",
                "hashedPassword",
                "John",
                "Doe",
                "1234567890",
                true
        );
        user.setRoles(roles);
        return user;
    }

    @Test
    void register_shouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe"
        );
        User user = createTestUser();

        when(userService.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(user);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertNotNull(response.user());
        verify(userService).createUser("test@example.com", "password123", "John", "Doe");
    }

    @Test
    void login_shouldReturnAuthResponseForValidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = createTestUser();

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        verify(jwtService).generateAccessToken(user);
    }

    @Test
    void login_shouldThrowExceptionForInvalidEmail() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowExceptionForInvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
        User user = createTestUser();

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowExceptionForDisabledAccount() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = createTestUser();
        user.setEnabled(false);

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class, () -> authService.login(request));
        assertEquals("ACCOUNT_DISABLED", exception.getCode());
    }

    @Test
    void refreshToken_shouldReturnNewAuthResponseForValidToken() {
        String refreshToken = "validRefreshToken";
        User user = createTestUser();

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn("test@example.com");
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("newRefreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.refreshToken(refreshToken);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertEquals("newRefreshToken", response.refreshToken());
    }

    @Test
    void refreshToken_shouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalidRefreshToken";

        when(jwtService.validateToken(invalidToken)).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.refreshToken(invalidToken));
        assertEquals("INVALID_REFRESH_TOKEN", exception.getCode());
    }

    @Test
    void refreshToken_shouldThrowExceptionWhenUserNotFound() {
        String refreshToken = "validRefreshToken";

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn("nonexistent@example.com");
        when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> authService.refreshToken(refreshToken));
        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = "validToken";

        when(jwtService.validateToken(token)).thenReturn(true);

        boolean isValid = authService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        String token = "invalidToken";

        when(jwtService.validateToken(token)).thenReturn(false);

        boolean isValid = authService.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void getUserByEmail_shouldReturnUser() {
        String email = "test@example.com";
        User user = createTestUser();

        when(userService.findByEmail(email)).thenReturn(Optional.of(user));

        User result = authService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void getUserByEmail_shouldThrowExceptionWhenUserNotFound() {
        String email = "nonexistent@example.com";

        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () -> authService.getUserByEmail(email));
        assertEquals("USER_NOT_FOUND", exception.getCode());
    }
}