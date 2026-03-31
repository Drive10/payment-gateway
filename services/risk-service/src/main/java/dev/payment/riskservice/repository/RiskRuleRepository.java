package dev.payment.riskservice.repository;

import dev.payment.riskservice.entity.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, UUID> {

    List<RiskRule> findByEnabledTrue();

    List<RiskRule> findByRuleType(String ruleType);

    List<RiskRule> findByPriority(Integer priority);

    List<RiskRule> findByAction(RiskRule.RiskAction action);

    List<RiskRule> findByEnabledTrueOrderByPriorityDesc();
}
