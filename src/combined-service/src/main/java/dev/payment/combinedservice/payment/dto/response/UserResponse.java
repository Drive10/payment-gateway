package dev.payment.combinedservice.payment.dto.response;

import java.util.UUID;
import java.util.Set;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        Set<String> roles
) {
}
