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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(UUID.randomUUID());
        testRole.setName("ROLE_USER");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setFullName("John Doe");
        testUser.setEnabled(true);
        testUser.setActive(true);
        testUser.setRoles(Set.of(testRole));

        registerRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void register_ValidRequest_ReturnsAuthResponse() {
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        assertEquals(3600L, response.expiresIn());
        assertNotNull(response.user());
        verify(userService).createUser(
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.firstName(),
                registerRequest.lastName(),
                registerRequest.role()
        );
    }

    @Test
    void register_UserServiceThrowsException_ThrowsAuthException() {
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new AuthException("Email already exists", "EMAIL_EXISTS"));

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("EMAIL_EXISTS", exception.getCode());
    }

    @Test
    void login_ValidCredentials_ReturnsAuthResponse() {
        when(userService.findByEmail(loginRequest.email())).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
    }

    @Test
    void login_InvalidEmail_ThrowsBadCredentialsException() {
        when(userService.findByEmail(loginRequest.email())).thenReturn(java.util.Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void login_InvalidPassword_ThrowsBadCredentialsException() {
        when(userService.findByEmail(loginRequest.email())).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );
    }

    @Test
    void login_DisabledAccount_ThrowsAuthException() {
        testUser.setEnabled(false);
        when(userService.findByEmail(loginRequest.email())).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(true);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("ACCOUNT_DISABLED", exception.getCode());
    }

    @Test
    void refreshToken_ValidToken_ReturnsNewAuthResponse() {
        String refreshToken = "validRefreshToken";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn(testUser.getEmail());
        when(userService.findByEmail(testUser.getEmail())).thenReturn(java.util.Optional.of(testUser));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("newAccessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("newRefreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        AuthResponse response = authService.refreshToken(refreshToken);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertEquals("newRefreshToken", response.refreshToken());
    }

    @Test
    void refreshToken_InvalidToken_ThrowsAuthException() {
        String refreshToken = "invalidRefreshToken";
        when(jwtService.validateToken(refreshToken)).thenReturn(false);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.refreshToken(refreshToken)
        );

        assertEquals("INVALID_REFRESH_TOKEN", exception.getCode());
    }

    @Test
    void refreshToken_UserNotFound_ThrowsAuthException() {
        String refreshToken = "validRefreshToken";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractEmail(refreshToken)).thenReturn("nonexistent@example.com");
        when(userService.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.refreshToken(refreshToken)
        );

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = "validToken";
        when(jwtService.validateToken(token)).thenReturn(true);

        boolean result = authService.validateToken(token);

        assertTrue(result);
        verify(jwtService).validateToken(token);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        String token = "invalidToken";
        when(jwtService.validateToken(token)).thenReturn(false);

        boolean result = authService.validateToken(token);

        assertFalse(result);
        verify(jwtService).validateToken(token);
    }

    @Test
    void getUserByEmail_ExistingUser_ReturnsUser() {
        String email = "test@example.com";
        when(userService.findByEmail(email)).thenReturn(java.util.Optional.of(testUser));

        User result = authService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void getUserByEmail_NonExistingUser_ThrowsAuthException() {
        String email = "nonexistent@example.com";
        when(userService.findByEmail(email)).thenReturn(java.util.Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> authService.getUserByEmail(email)
        );

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }
}