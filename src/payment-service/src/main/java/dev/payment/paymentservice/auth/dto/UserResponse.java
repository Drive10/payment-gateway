package dev.payment.paymentservice.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.payment.paymentservice.auth.entity.User;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        String fullName,
        String role
) {
    public static UserResponse from(User user) {
        List<String> userRoles = user.getRoles().stream()
                .map(role -> role.getName().replace("ROLE_", ""))
                .collect(Collectors.toList());
        String primaryRole = determinePrimaryRole(userRoles);
        String fullName = user.getFirstName() + " " + user.getLastName();
        
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                userRoles,
                fullName,
                primaryRole
        );
    }

    private static String determinePrimaryRole(List<String> userRoles) {
        if (userRoles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (userRoles.isEmpty()) {
            return "USER";
        }
        return userRoles.get(0);
    }
}
