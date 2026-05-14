package dev.payment.authservice;

import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.Role.RoleName;
import dev.payment.authservice.entity.User;
import dev.payment.authservice.repository.RoleRepository;
import dev.payment.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding development data...");

        Role adminRole = getOrCreateRole(RoleName.ADMIN);
        Role merchantRole = getOrCreateRole(RoleName.MERCHANT);
        Role customerRole = getOrCreateRole(RoleName.CUSTOMER);

        createUser("admin@payflow.dev", "Password123", "Admin", "User", adminRole);
        createUser("merchant@test.com", "Password123", "Test", "Merchant", merchantRole);
        createUser("dev@test.com", "Password123", "Dev", "User", customerRole);

        log.info("Seed data ready: admin@payflow.dev / merchant@test.com / dev@test.com (password: Password123)");
    }

    private Role getOrCreateRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder().name(name).build();
            role = roleRepository.save(role);
            log.info("Created role: {}", name);
            return role;
        });
    }

    private void createUser(String email, String password, String firstName, String lastName, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
            u -> log.info("User exists: {}", email),
            () -> {
                User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .firstName(firstName)
                    .lastName(lastName)
                    .enabled(true)
                    .build();
                user.getRoles().add(role);
                userRepository.save(user);
                log.info("Created user: {} ({})", email, role.getName());
            }
        );
    }
}
