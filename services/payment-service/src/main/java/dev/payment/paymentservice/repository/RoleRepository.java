package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}
