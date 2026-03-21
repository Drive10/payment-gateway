package dev.payment.authservice.repository;

import dev.payment.authservice.domain.AccessAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessAuditRepository extends JpaRepository<AccessAudit, Long> {
    Page<AccessAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
