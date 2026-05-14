package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.SubscriptionRequest;
import dev.payment.paymentservice.entity.Plan;
import dev.payment.paymentservice.entity.Subscription;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;

    @Transactional
    public Subscription createSubscription(SubscriptionRequest request, String merchantId) {
        Plan plan = planService.getPlan(request.getPlanId());
        if (!"active".equals(plan.getStatus())) {
            throw PaymentException.badRequest("Plan is not active");
        }

        Subscription.SubscriptionStatus initialStatus = Subscription.SubscriptionStatus.ACTIVE;
        Instant periodStart = Instant.now();
        Instant periodEnd = calculatePeriodEnd(periodStart, plan.getInterval());

        if (request.getTrialPeriodDays() != null && request.getTrialPeriodDays() > 0) {
            initialStatus = Subscription.SubscriptionStatus.TRIALING;
            periodEnd = periodStart.plus(request.getTrialPeriodDays(), ChronoUnit.DAYS);
        }

        Subscription subscription = Subscription.builder()
                .merchantId(merchantId)
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerName())
                .planId(plan.getId().toString())
                .planName(plan.getName())
                .amount(plan.getAmount())
                .currency(plan.getCurrency())
                .interval(plan.getInterval())
                .status(initialStatus)
                .paymentMethodToken(request.getPaymentMethodToken())
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .trialEnd(request.getTrialPeriodDays() != null && request.getTrialPeriodDays() > 0 ? periodEnd : null)
                .billingCycles(request.getBillingCycles())
                .billingCyclesDone(0)
                .build();

        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getMerchantSubscriptions(String merchantId) {
        return subscriptionRepository.findByMerchantId(merchantId);
    }

    public Subscription getSubscription(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> PaymentException.notFound("Subscription not found"));
    }

    @Transactional
    public Subscription cancelSubscription(String subscriptionId) {
        Subscription sub = getSubscription(subscriptionId);
        if (sub.getStatus() == Subscription.SubscriptionStatus.CANCELLED) {
            throw PaymentException.badRequest("Subscription is already cancelled");
        }
        sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription pauseSubscription(String subscriptionId) {
        Subscription sub = getSubscription(subscriptionId);
        if (sub.getStatus() != Subscription.SubscriptionStatus.ACTIVE) {
            throw PaymentException.badRequest("Only active subscriptions can be paused");
        }
        sub.setStatus(Subscription.SubscriptionStatus.PAUSED);
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription resumeSubscription(String subscriptionId) {
        Subscription sub = getSubscription(subscriptionId);
        if (sub.getStatus() != Subscription.SubscriptionStatus.PAUSED) {
            throw PaymentException.badRequest("Only paused subscriptions can be resumed");
        }
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        return subscriptionRepository.save(sub);
    }

    private Instant calculatePeriodEnd(Instant start, String interval) {
        return switch (interval.toLowerCase()) {
            case "daily" -> start.plus(1, ChronoUnit.DAYS);
            case "weekly" -> start.plus(7, ChronoUnit.DAYS);
            case "monthly" -> start.plus(30, ChronoUnit.DAYS);
            case "yearly" -> start.plus(365, ChronoUnit.DAYS);
            default -> start.plus(30, ChronoUnit.DAYS);
        };
    }
}
