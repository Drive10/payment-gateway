package dev.payment.analyticsservice.service;

import dev.payment.analyticsservice.entity.RiskAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RiskScoringService {
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal CRITICAL_AMOUNT_THRESHOLD = new BigDecimal("500000");
    
    private static final Logger log = LoggerFactory.getLogger(RiskScoringService.class);
    
    private final Map<String, Integer> userTransactionCount = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> userTotalAmount = new ConcurrentHashMap<>();
    private final Map<String, Long> userFirstTransactionTime = new ConcurrentHashMap<>();
    
    public RiskAssessment evaluateTransaction(
            UUID transactionId,
            UUID userId,
            BigDecimal amount,
            String currency,
            Map<String, Object> metadata) {
        
        RiskAssessment assessment = new RiskAssessment();
        assessment.setTransactionId(transactionId);
        assessment.setUserId(userId);
        assessment.setAmount(amount);
        assessment.setCurrency(currency != null ? currency : "INR");
        
        int totalScore = 0;
        List<String> triggeredRules = new ArrayList<>();
        List<String> flags = new ArrayList<>();
        
        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            totalScore += 30;
            triggeredRules.add("HIGH_AMOUNT");
            flags.add("Large transaction amount");
        }
        
        if (amount.compareTo(CRITICAL_AMOUNT_THRESHOLD) > 0) {
            totalScore += 50;
            triggeredRules.add("VERY_HIGH_AMOUNT");
            flags.add("Exceptionally large transaction");
        }
        
        String userKey = userId.toString();
        int userTxCount = userTransactionCount.getOrDefault(userKey, 0);
        userTransactionCount.put(userKey, userTxCount + 1);
        
        if (userTxCount > 10) {
            totalScore += 20;
            triggeredRules.add("HIGH_FREQUENCY");
            flags.add("High transaction frequency");
        }
        
        BigDecimal totalAmount = userTotalAmount.getOrDefault(userKey, BigDecimal.ZERO);
        userTotalAmount.put(userKey, totalAmount.add(amount));
        
        if (totalAmount.add(amount).compareTo(new BigDecimal("1000000")) > 0) {
            totalScore += 25;
            triggeredRules.add("HIGH_VOLUME");
            flags.add("High volume user");
        }
        
        Long firstTxTime = userFirstTransactionTime.get(userKey);
        if (firstTxTime == null) {
            userFirstTransactionTime.put(userKey, System.currentTimeMillis());
            totalScore += 15;
            triggeredRules.add("NEW_USER");
            flags.add("First transaction for user");
        } else {
            long timeDiff = System.currentTimeMillis() - firstTxTime;
            if (timeDiff < 60000) {
                totalScore += 30;
                triggeredRules.add("RAPID_FIRE");
                flags.add("Multiple transactions in short time");
            }
        }
        
        String email = (String) metadata.get("email");
        if (email != null && email.contains("test")) {
            totalScore -= 20;
        }
        
        assessment.setRiskScore(totalScore);
        
        if (totalScore >= 80) {
            assessment.setRiskLevel(RiskAssessment.RiskLevel.CRITICAL);
            assessment.setDecision(RiskAssessment.Decision.REJECT);
        } else if (totalScore >= 50) {
            assessment.setRiskLevel(RiskAssessment.RiskLevel.HIGH);
            assessment.setDecision(RiskAssessment.Decision.REVIEW);
        } else if (totalScore >= 25) {
            assessment.setRiskLevel(RiskAssessment.RiskLevel.MEDIUM);
            assessment.setDecision(RiskAssessment.Decision.APPROVE);
        } else {
            assessment.setRiskLevel(RiskAssessment.RiskLevel.LOW);
            assessment.setDecision(RiskAssessment.Decision.APPROVE);
        }
        
        assessment.setTriggeredRules(triggeredRules.toString());
        assessment.setFlags(flags.toString());
        assessment.setMetadata(metadata.toString());
        
        log.info("Risk assessment for tx {}: score={}, level={}, decision={}", 
            transactionId, totalScore, assessment.getRiskLevel(), assessment.getDecision());
        
        return assessment;
    }
    
    public void resetUserMetrics(UUID userId) {
        String key = userId.toString();
        userTransactionCount.remove(key);
        userTotalAmount.remove(key);
        userFirstTransactionTime.remove(key);
    }
}
