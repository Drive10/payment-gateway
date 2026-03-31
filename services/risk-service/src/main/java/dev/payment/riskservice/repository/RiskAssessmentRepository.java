package dev.payment.riskservice.repository;

import dev.payment.riskservice.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findByUserId(UUID userId);

    List<RiskAssessment> findByTransactionId(UUID transactionId);

    List<RiskAssessment> findByRiskLevel(RiskAssessment.RiskLevel riskLevel);

    List<RiskAssessment> findByDecision(RiskAssessment.Decision decision);

    @Query("SELECT r FROM RiskAssessment r WHERE r.userId = :userId AND r.assessedAt >= :since ORDER BY r.assessedAt DESC")
    List<RiskAssessment> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(r) FROM RiskAssessment r WHERE r.decision = :decision AND r.assessedAt >= :since")
    Long countByDecisionSince(@Param("decision") RiskAssessment.Decision decision, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(r.riskScore) FROM RiskAssessment r WHERE r.assessedAt >= :since")
    Double averageRiskScoreSince(@Param("since") LocalDateTime since);
}
