package dev.payment.authservice.repository;

import dev.payment.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(@Param("email") String email);
    boolean existsByEmail(@Param("email") String email);
}
