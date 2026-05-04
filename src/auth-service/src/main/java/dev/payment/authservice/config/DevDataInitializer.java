package dev.payment.authservice;

import dev.payment.authservice.entity.User;
import dev.payment.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("DevDataInitializer running...");
        userRepository.findByEmail("dev@test.com").ifPresentOrElse(
            u -> { log.info("Dev user already exists"); },
            () -> {
                User user = new User();
                user.setEmail("dev@test.com");
                user.setPassword(passwordEncoder.encode("Password123"));
                user.setFirstName("Dev");
                user.setLastName("User");
                user.setEnabled(true);
                userRepository.save(user);
                log.info("Dev user created");
            }
        );
    }
}