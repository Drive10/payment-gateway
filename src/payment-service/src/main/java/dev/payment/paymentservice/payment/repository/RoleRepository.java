package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.Role;
import dev.payment.paymentservice.payment.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}
