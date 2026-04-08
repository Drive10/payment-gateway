package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.dto.response.UserResponse;
import dev.payment.paymentservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse syncUser(String email) {
        log.info("event=syncing_user email={}", email);
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> toUserResponse(user))
                .orElseGet(() -> {
                    User user = createUserFromExternalAuth(email);
                    return toUserResponse(userRepository.save(user));
                });
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(createUserFromExternalAuth(email)));
    }

    private User createAndReturnUser(String email) {
        User user = createUserFromExternalAuth(email);
        return userRepository.save(user);
    }

    private User createUserFromExternalAuth(String email) {
        log.info("event=user_not_found_locally email={} attempting sync from auth-service", email);
        User user = new User();
        user.setEmail(email.toLowerCase());
        String nameSeed = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        user.setFirstName(nameSeed);
        user.setLastName("User");
        user.setFullName(nameSeed);
        user.setPassword("");
        user.setPasswordHash("");
        user.setActive(true);
        user.setEnabled(true);
        return user;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())
        );
    }
}
