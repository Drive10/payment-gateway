package dev.payment.authservice.service;

import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.User;
import dev.payment.authservice.exception.AuthException;
import dev.payment.authservice.repository.RoleRepository;
import dev.payment.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(UUID.randomUUID());
        userRole.setName("ROLE_USER");

        adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName("ROLE_ADMIN");
    }

    @Test
    void createUser_ValidRequest_ReturnsUser() {
        RegisterRequestBuilder builder = new RegisterRequestBuilder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .role("USER");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser(
                builder.build().email(),
                builder.build().password(),
                builder.build().firstName(),
                builder.build().lastName(),
                builder.build().role()
        );

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsAuthException() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        AuthException exception = assertThrows(
                AuthException.class,
                () -> userService.createUser("existing@example.com", "password", "John", "Doe", "USER")
        );

        assertEquals("EMAIL_EXISTS", exception.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_DefaultRole_UserRoleAssigned() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("test@example.com", "password", "John", "Doe", null);

        assertNotNull(result);
        assertTrue(result.getRoles().contains(userRole));
    }

    @Test
    void createUser_CustomRole_CustomRoleAssigned() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("admin@example.com", "password", "Admin", "User", "ADMIN");

        assertNotNull(result);
        assertTrue(result.getRoles().contains(adminRole));
    }

    @Test
    void createUser_RoleNotFound_DefaultsToUserRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("test@example.com", "password", "John", "Doe", "NONEXISTENT");

        assertNotNull(result);
        assertTrue(result.getRoles().contains(userRole));
    }

    @Test
    void getUserById_ExistingId_ReturnsUser() {
        UUID userId = UUID.randomUUID();
        User expectedUser = new User();
        expectedUser.setId(userId);
        expectedUser.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserById_NonExistingId_ThrowsAuthException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(
                AuthException.class,
                () -> userService.getUserById(userId)
        );

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    void findByEmail_ExistingEmail_ReturnsUser() {
        String email = "test@example.com";
        User expectedUser = new User();
        expectedUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(expectedUser));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
    }

    @Test
    void findByEmail_NonExistingEmail_ReturnsEmpty() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isEmpty());
    }

    @Test
    void createUser_SetsFullNameCorrectly() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("test@example.com", "password", "John", "Doe", "USER");

        assertEquals("John Doe", result.getFullName());
    }

    @Test
    void createUser_EnabledByDefault() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("test@example.com", "password", "John", "Doe", "USER");

        assertTrue(result.isEnabled());
    }

    @Test
    void createUser_ActiveByDefault() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User result = userService.createUser("test@example.com", "password", "John", "Doe", "USER");

        assertTrue(result.isActive());
    }

    private static class RegisterRequestBuilder {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String role;

        RegisterRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        RegisterRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        RegisterRequestBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        RegisterRequestBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        RegisterRequestBuilder role(String role) {
            this.role = role;
            return this;
        }

        dev.payment.authservice.dto.RegisterRequest build() {
            return new dev.payment.authservice.dto.RegisterRequest(email, password, firstName, lastName, role);
        }
    }
}