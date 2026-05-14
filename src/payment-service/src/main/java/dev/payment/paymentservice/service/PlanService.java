package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.PlanRequest;
import dev.payment.paymentservice.entity.Plan;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {
    private final PlanRepository planRepository;

    @Transactional
    public Plan createPlan(PlanRequest request, String merchantId) {
        Plan plan = Plan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .interval(request.getInterval())
                .trialPeriodDays(request.getTrialPeriodDays())
                .merchantId(merchantId)
                .status("active")
                .build();
        return planRepository.save(plan);
    }

    public List<Plan> getMerchantPlans(String merchantId) {
        return planRepository.findByMerchantId(merchantId);
    }

    public Plan getPlan(String planId) {
        return planRepository.findById(java.util.UUID.fromString(planId))
                .orElseThrow(() -> PaymentException.notFound("Plan not found"));
    }

    @Transactional
    public Plan updatePlan(String planId, PlanRequest request) {
        Plan plan = getPlan(planId);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setAmount(request.getAmount());
        plan.setCurrency(request.getCurrency().toUpperCase());
        plan.setInterval(request.getInterval());
        plan.setTrialPeriodDays(request.getTrialPeriodDays());
        return planRepository.save(plan);
    }

    @Transactional
    public Plan deactivatePlan(String planId) {
        Plan plan = getPlan(planId);
        plan.setStatus("inactive");
        return planRepository.save(plan);
    }
}
