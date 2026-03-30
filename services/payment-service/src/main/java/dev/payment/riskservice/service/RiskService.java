package dev.payment.riskservice.service;

import dev.payment.riskservice.domain.Decision;
import dev.payment.riskservice.domain.RiskAssessment;
import dev.payment.riskservice.dto.request.CreateAssessmentRequest;
import dev.payment.riskservice.dto.response.AssessmentResponse;
import dev.payment.riskservice.repository.RiskAssessmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskService {
    private final RiskAssessmentRepository riskAssessmentRepository;
    public RiskService(RiskAssessmentRepository riskAssessmentRepository) { this.riskAssessmentRepository = riskAssessmentRepository; }

    public AssessmentResponse assess(CreateAssessmentRequest request) {
        int score = calculateScore(request);
        Decision decision = score >= 80 ? Decision.DECLINE : score >= 50 ? Decision.REVIEW : Decision.APPROVE;
        RiskAssessment assessment = new RiskAssessment();
        assessment.setMerchantReference(request.merchantReference());
        assessment.setAmount(request.amount());
        assessment.setCountryCode(request.countryCode().toUpperCase());
        assessment.setVelocityCount(request.velocityCount());
        assessment.setRiskScore(score);
        assessment.setDecision(decision);
        assessment.setReasons(buildReasons(score, request.velocityCount()));
        riskAssessmentRepository.save(assessment);
        return toResponse(assessment);
    }

    public List<AssessmentResponse> getAssessments(Decision decision) {
        List<RiskAssessment> assessments = decision == null ? riskAssessmentRepository.findAll() : riskAssessmentRepository.findByDecision(decision);
        return assessments.stream().map(this::toResponse).toList();
    }

    private int calculateScore(CreateAssessmentRequest request) {
        int score = 10;
        if (request.amount().doubleValue() > 100000) score += 45;
        if (!"IN".equalsIgnoreCase(request.countryCode())) score += 20;
        score += Math.min(request.velocityCount() * 5, 30);
        return Math.min(score, 100);
    }

    private String buildReasons(int score, int velocityCount) {
        if (score >= 80) return "High ticket size or risky geography combined with elevated velocity " + velocityCount;
        if (score >= 50) return "Requires analyst review due to medium-high exposure";
        return "Low risk pattern";
    }

    private AssessmentResponse toResponse(RiskAssessment assessment) {
        return new AssessmentResponse(assessment.getId(), assessment.getMerchantReference(), assessment.getAmount(), assessment.getCountryCode(), assessment.getVelocityCount(), assessment.getRiskScore(), assessment.getDecision().name(), assessment.getReasons(), assessment.getCreatedAt());
    }
}
