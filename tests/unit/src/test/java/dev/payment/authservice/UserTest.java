package dev.payment.authservice;

import dev.payment.authservice.Role;
import dev.payment.authservice.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        user = new User(
            "test@example.com",
            "hashedPassword123",
            "John",
            "Doe",
            "1234567890",
            true
        );
        
        role = new Role();
        role.setName("ROLE_USER");
        role.setDescription("Standard user role");
    }

    @Nested
    @DisplayName("User Creation")
    class UserCreation {
        
        @Test
        @DisplayName("should create user with all fields")
        void shouldCreateUserWithAllFields() {
            assertNotNull(user);
            assertEquals("test@example.com", user.getEmail());
            assertEquals("hashedPassword123", user.getPasswordHash());
            assertEquals("John", user.getFirstName());
            assertEquals("Doe", user.getLastName());
            assertEquals("1234567890", user.getPhone());
            assertTrue(user.isEnabled());
        }

        @Test
        @DisplayName("should have empty roles by default")
        void shouldHaveEmptyRolesByDefault() {
            assertNotNull(user.getRoles());
            assertTrue(user.getRoles().isEmpty());
        }
    }

    @Nested
    @DisplayName("Role Management")
    class RoleManagement {

        @Test
        @DisplayName("should add role to user")
        void shouldAddRoleToUser() {
            user.addRole(role);
            
            assertEquals(1, user.getRoles().size());
            assertTrue(user.getRoles().contains(role));
        }

        @Test
        @DisplayName("should add role bidirectionally")
        void shouldAddRoleBidirectionally() {
            user.addRole(role);
            
            assertTrue(role.getUsers().contains(user));
        }

        @Test
        @DisplayName("should remove role from user")
        void shouldRemoveRoleFromUser() {
            user.addRole(role);
            user.removeRole(role);
            
            assertTrue(user.getRoles().isEmpty());
            assertFalse(role.getUsers().contains(user));
        }

        @Test
        @DisplayName("should handle multiple roles")
        void shouldHandleMultipleRoles() {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            
            user.addRole(role);
            user.addRole(adminRole);
            
            assertEquals(2, user.getRoles().size());
        }
    }

    @Nested
    @DisplayName("Entity Auditing")
    class EntityAuditing {

        @Test
        @DisplayName("should have null id initially")
        void shouldHaveNullIdInitially() {
            assertNull(user.getId());
        }

        @Test
        @DisplayName("should have null timestamps initially")
        void shouldHaveNullTimestampsInitially() {
            assertNull(user.getCreatedAt());
            assertNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("should have null version initially")
        void shouldHaveNullVersionInitially() {
            assertNull(user.getVersion());
        }
    }
}
