package dev.payment.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.authservice.dto.AuthResponse;
import dev.payment.authservice.dto.LoginRequest;
import dev.payment.authservice.dto.RegisterRequest;
import dev.payment.authservice.dto.UserResponse;
import dev.payment.authservice.exception.AuthException;
import dev.payment.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        validLoginRequest = new LoginRequest("test@example.com", "password123");

        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(),
                "test@example.com",
                "John",
                "Doe",
                java.util.List.of("USER"),
                "John Doe",
                "USER"
        );

        authResponse = AuthResponse.of(
                "accessToken",
                "refreshToken",
                3600L,
                userResponse
        );
    }

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "invalid-email",
                "password123",
                "John",
                "Doe",
                "USER"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "test@example.com",
                "123",
                "John",
                "Doe",
                "USER"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_MissingFirstName_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "",
                "Doe",
                "USER"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ServiceThrowsException_ReturnsError() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AuthException("Email already exists", "EMAIL_EXISTS"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ValidRequest_ReturnsOk() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }

    @Test
    void login_InvalidEmail_ReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("invalid-email", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_EmptyPassword_ReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("test@example.com", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_DisabledAccount_ReturnsBadRequest() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException("Account is disabled", "ACCOUNT_DISABLED"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_ValidToken_ReturnsOk() throws Exception {
        when(authService.refreshToken("validRefreshToken")).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"validRefreshToken\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    void refresh_MissingToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_BlankToken_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_InvalidToken_ReturnsBadRequest() throws Exception {
        when(authService.refreshToken("invalidToken"))
                .thenThrow(new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"invalidToken\"}"))
                .andExpect(status().isBadRequest());
    }
}