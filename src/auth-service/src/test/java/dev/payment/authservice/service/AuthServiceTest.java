package dev.payment.authservice.service;

import dev.payment.authservice.dto.*;
import dev.payment.authservice.entity.*;
import dev.payment.authservice.entity.Role.RoleName;
import dev.payment.authservice.repository.*;
import dev.payment.authservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "dGhpcyBpcyBhIGp3dCBzZWNyZXQga2V5IGZvciBwYXlmbG93IGRldiBlbnZpcm9ubWVudA==");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiration", 86400000L);

        authService = new AuthService(userRepository, roleRepository, refreshTokenRepository, passwordEncoder, tokenProvider);
    }

    @Test
    void registerCustomer_ShouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        Role customerRole = Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).build();
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(savedUser));

        TokenResponse response = authService.registerCustomer(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertTrue(response.getAccessToken().length() > 0);
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void registerCustomer_WithExistingEmail_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("Existing");
        request.setLastName("User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.registerCustomer(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        String email = "user@example.com";
        String password = "password123";
        String encodedPassword = passwordEncoder.encode(password);

        Role role = Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(encodedPassword)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Set.of(role))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        TokenResponse response = authService.login(email, password);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertTrue(response.getAccessToken().length() > 0);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowException() {
        String email = "user@example.com";
        String encodedPassword = passwordEncoder.encode("correct-password");

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(encodedPassword)
                .enabled(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(email, "wrong-password"));
    }

    @Test
    void login_WithDisabledAccount_ShouldThrowException() {
        String email = "disabled@example.com";
        String password = "password123";
        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(encodedPassword)
                .enabled(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> authService.login(email, password));
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewTokens() throws Exception {
        String email = "user@example.com";

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .enabled(true)
                .build();

        String refreshTokenValue = tokenProvider.generateRefreshToken(email);

        RefreshToken tokenEntity = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        TokenResponse response = authService.refreshToken(refreshTokenValue);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertTrue(response.getAccessToken().length() > 0);
        assertTrue(tokenEntity.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowException() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.refreshToken("invalid"));
    }

    @Test
    void login_WithNonExistentEmail_ShouldThrowException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login("unknown@example.com", "password"));
    }
}
