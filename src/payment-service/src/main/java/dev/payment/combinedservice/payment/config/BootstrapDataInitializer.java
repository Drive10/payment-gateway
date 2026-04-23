package dev.payment.combinedservice.payment.config;

import dev.payment.combinedservice.payment.domain.Role;
import dev.payment.combinedservice.payment.domain.User;
import dev.payment.combinedservice.payment.domain.enums.RoleName;
import dev.payment.combinedservice.payment.repository.RoleRepository;
import dev.payment.combinedservice.payment.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile({"dev", "stage"})
public class BootstrapDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    @Bean
    ApplicationRunner bootstrapPaymentData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${application.bootstrap.admin-email:}") String adminEmail,
            @Value("${application.bootstrap.admin-password:}") String adminPassword
    ) {
        return args -> {
            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(RoleName.ADMIN);
                        return roleRepository.save(role);
                    });
            Role userRole = roleRepository.findByName(RoleName.USER)
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName(RoleName.USER);
                        return roleRepository.save(role);
                    });

            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.info("event=bootstrap_admin_skipped reason=credentials_not_configured");
                return;
            }

            userRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(
                    existing -> log.info("event=bootstrap_admin_exists email={}", existing.getEmail()),
                    () -> {
                        User admin = new User();
                        admin.setEmail(adminEmail.toLowerCase());
                        admin.setFirstName("Platform");
                        admin.setLastName("Admin");
                        admin.setFullName("Platform Admin");
                        admin.setPassword(passwordEncoder.encode(adminPassword));
                        admin.setPasswordHash(admin.getPassword());
                        admin.setEnabled(true);
                        admin.getRoles().add(adminRole);
                        admin.getRoles().add(userRole);
                        userRepository.save(admin);
                        log.info("event=bootstrap_admin_created email={}", admin.getEmail());
                    }
            );
        };
    }
}
