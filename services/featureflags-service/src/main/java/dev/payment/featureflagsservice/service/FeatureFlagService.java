package dev.payment.featureflagsservice.service;

import dev.payment.featureflagsservice.entity.FeatureFlag;
import dev.payment.featureflagsservice.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository repository;
    private final Random random = new Random();

    public FeatureFlagService(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    public boolean isFeatureEnabled(String key) {
        return repository.findByKey(key)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    public boolean isFeatureEnabledForUser(String key, String userId) {
        return repository.findByKey(key)
                .map(flag -> checkRollout(flag, userId))
                .orElse(false);
    }

    private boolean checkRollout(FeatureFlag flag, String userId) {
        if (!flag.isEnabled()) {
            return false;
        }

        if (flag.getUserIds() != null && flag.getUserIds().contains(userId)) {
            return true;
        }

        if (flag.getRolloutPercentage() == null || flag.getRolloutPercentage() >= 100) {
            return flag.isEnabled();
        }

        return random.nextInt(100) < flag.getRolloutPercentage();
    }
}
