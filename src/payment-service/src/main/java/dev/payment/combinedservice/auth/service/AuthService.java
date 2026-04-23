package dev.payment.combinedservice.auth.service;

import dev.payment.combinedservice.auth.dto.AuthResponse;
import dev.payment.combinedservice.auth.dto.LoginRequest;
import dev.payment.combinedservice.auth.dto.RegisterRequest;
import dev.payment.combinedservice.auth.dto.UserResponse;
import dev.payment.combinedservice.auth.entity.User;
import dev.payment.combinedservice.auth.entity.UserPrincipal;
import dev.payment.combinedservice.auth.exception.AuthException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserService userService, JwtService jwtService, 
                       PasswordEncoder passwordEncoder, TokenBlacklistService tokenBlacklistService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.role()
        );

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration(),
                UserResponse.from(user)
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled", "ACCOUNT_DISABLED");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration(),
                UserResponse.from(user)
        );
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpiration(),
                UserResponse.from(user)
        );
    }

    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    public User getUserByEmail(String email) {
        return userService.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                if (jwtService.validateToken(refreshToken)) {
                    long expiration = jwtService.getRefreshTokenExpiration();
                    tokenBlacklistService.blacklistToken(refreshToken, expiration);
                }
            } catch (Exception e) {
                // Token invalid anyway, ignore
            }
        }
    }

    public UserResponse getCurrentUser(String email) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));
        return UserResponse.from(user);
    }
}
