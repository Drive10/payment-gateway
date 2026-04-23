package dev.payment.combinedservice.payment.repository;

import dev.payment.combinedservice.payment.domain.Role;
import dev.payment.combinedservice.payment.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}
