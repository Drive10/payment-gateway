package dev.payment.notificationservice.infrastructure.persistence.repository;

import dev.payment.notificationservice.domain.entities.NotificationChannel;
import dev.payment.notificationservice.domain.entities.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    Optional<Template> findByTemplateKey(String templateKey);

    List<Template> findByChannel(NotificationChannel channel);

    List<Template> findByActiveTrue();

    List<Template> findByChannelAndActiveTrue(NotificationChannel channel);
}
