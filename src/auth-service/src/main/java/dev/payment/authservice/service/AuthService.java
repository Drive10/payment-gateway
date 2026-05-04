package dev.payment.authservice.service;

import dev.payment.authservice.dto.*;
import dev.payment.authservice.entity.*;
import dev.payment.authservice.repository.*;
import dev.payment.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public TokenResponse registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role customerRole = roleRepository.findByName(Role.RoleName.CUSTOMER)
            .orElseGet(() -> {
                Role role = Role.builder().name(Role.RoleName.CUSTOMER).build();
                return roleRepository.save(role);
            });

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .roles(Set.of(customerRole))
            .enabled(true)
            .build();

        user = userRepository.save(user);
        return generateTokens(user.getEmail(), "CUSTOMER");
    }

    public TokenResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        String role = user.getRoles().stream()
            .findFirst()
            .map(r -> r.getName().name())
            .orElse("CUSTOMER");

        return generateTokens(user.getEmail(), role);
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!token.isValid()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String email = token.getUser().getEmail();
        return generateTokens(email, "CUSTOMER");
    }

    private TokenResponse generateTokens(String email, String role) {
        String accessToken = tokenProvider.generateAccessToken(email, role);
        String refreshToken = tokenProvider.generateRefreshToken(email);

        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = User.builder()
                    .email(email)
                    .firstName("System")
                    .lastName("Account")
                    .password(passwordEncoder.encode("dummy-password"))
                    .enabled(true)
                    .build();
                return userRepository.save(newUser);
            });

        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .token(refreshToken)
            .user(user)
            .expiresAt(Instant.now().plusMillis(tokenProvider.getAccessTokenExpiration() * 24))
            .revoked(false)
            .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(tokenProvider.getAccessTokenExpiration())
            .build();
    }
}