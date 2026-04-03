package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.dto.response.UserResponse;
import dev.payment.paymentservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    @Test
    void syncUser_shouldReturnExistingUser() {
        User user = createTestUser();
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        UserResponse result = authService.syncUser("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    void syncUser_shouldCreateNewUserWhenNotFound() {
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse result = authService.syncUser("new@example.com");

        assertNotNull(result);
        assertEquals("new@example.com", result.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getCurrentUser_shouldReturnExistingUser() {
        User user = createTestUser();
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        User result = authService.getCurrentUser("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getCurrentUser_shouldCreateNewUserWhenNotFound() {
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = authService.getCurrentUser("new@example.com");

        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPassword("hashed");
        user.setActive(true);
        return user;
    }
}
