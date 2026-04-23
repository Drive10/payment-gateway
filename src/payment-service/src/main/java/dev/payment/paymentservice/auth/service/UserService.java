package dev.payment.paymentservice.auth.service;

import dev.payment.paymentservice.auth.entity.Role;
import dev.payment.paymentservice.auth.entity.User;
import dev.payment.paymentservice.auth.exception.AuthException;
import dev.payment.paymentservice.auth.repository.RoleRepository;
import dev.payment.paymentservice.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String email, String password, String firstName, String lastName, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("Email already exists", "EMAIL_EXISTS");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setFullName(((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim());
        user.setEnabled(true);
        user.setActive(true);

        String roleName = (role != null && !role.isBlank()) ? role : "USER";
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }
        Optional<Role> userRole = roleRepository.findByName(roleName);
        userRole.ifPresentOrElse(
                user::addRole,
                () -> roleRepository.findByName("ROLE_USER").ifPresent(user::addRole)
        );

        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
