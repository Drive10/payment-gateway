package dev.payment.riskservice.repository;

import dev.payment.riskservice.domain.Decision;
import dev.payment.riskservice.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {
    List<RiskAssessment> findByDecision(Decision decision);
}
