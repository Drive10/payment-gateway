package dev.payment.notificationservice.repository;

import dev.payment.notificationservice.entity.NotificationChannel;
import dev.payment.notificationservice.entity.Template;
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
