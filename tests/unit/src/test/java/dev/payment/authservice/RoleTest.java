package dev.payment.authservice;

import dev.payment.authservice.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
    }

    @Nested
    @DisplayName("Role Creation")
    class RoleCreation {

        @Test
        @DisplayName("should create role with name")
        void shouldCreateRoleWithName() {
            role.setName("ROLE_USER");
            role.setDescription("Standard user role");
            
            assertEquals("ROLE_USER", role.getName());
            assertEquals("Standard user role", role.getDescription());
        }

        @Test
        @DisplayName("should have empty users by default")
        void shouldHaveEmptyUsersByDefault() {
            assertNotNull(role.getUsers());
            assertTrue(role.getUsers().isEmpty());
        }
    }

    @Nested
    @DisplayName("Role Equality")
    class RoleEquality {

        @Test
        @DisplayName("should compare roles by name")
        void shouldCompareRolesByName() {
            Role role1 = new Role();
            role1.setName("ROLE_USER");
            
            Role role2 = new Role();
            role2.setName("ROLE_USER");
            
            Role role3 = new Role();
            role3.setName("ROLE_ADMIN");
            
            assertEquals(role1, role2);
            assertNotEquals(role1, role3);
        }
    }
}
