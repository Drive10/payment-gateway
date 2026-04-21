package dev.payment.common.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "management", name = "endpoint.health.show-details", havingValue = "always")
    public HealthIndicator livenessHealthIndicator() {
        return () -> Health.up().build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "management", name = "endpoint.health.show-details", havingValue = "always")
    public HealthIndicator readinessHealthIndicator() {
        return () -> Health.up().build();
    }
}