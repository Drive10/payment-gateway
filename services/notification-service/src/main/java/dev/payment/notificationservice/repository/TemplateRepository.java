package dev.payment.notificationservice.repository;

import dev.payment.notificationservice.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findByTemplateCode(String templateCode);
}
