package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
