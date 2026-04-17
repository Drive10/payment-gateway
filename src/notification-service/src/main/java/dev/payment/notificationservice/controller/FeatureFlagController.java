package dev.payment.notificationservice.controller;

import dev.payment.notificationservice.entity.FeatureFlag;
import dev.payment.notificationservice.repository.FeatureFlagRepository;
import dev.payment.notificationservice.service.FeatureFlagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagRepository repository;
    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagRepository repository, FeatureFlagService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getAllFlags() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getFlag(@PathVariable String key) {
        return repository.findByKey(key)
                .map(flag -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("key", flag.getKey());
                    response.put("enabled", flag.isEnabled());
                    response.put("rolloutPercentage", flag.getRolloutPercentage());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{key}")
    public ResponseEntity<Map<String, Boolean>> checkFlag(
            @PathVariable String key,
            @RequestParam(required = false) String userId) {
        boolean enabled = userId != null 
                ? service.isFeatureEnabledForUser(key, userId) 
                : service.isFeatureEnabled(key);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("enabled", enabled);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{key}/enable")
    public ResponseEntity<FeatureFlag> enableFlag(@PathVariable String key) {
        return repository.findByKey(key)
                .map(flag -> {
                    flag.setEnabled(true);
                    return ResponseEntity.ok(repository.save(flag));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{key}/disable")
    public ResponseEntity<FeatureFlag> disableFlag(@PathVariable String key) {
        return repository.findByKey(key)
                .map(flag -> {
                    flag.setEnabled(false);
                    return ResponseEntity.ok(repository.save(flag));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
