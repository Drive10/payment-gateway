package dev.payment.paymentservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class HealthConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(DataSource dataSource) {
        return () -> {
            Health.Builder builder = new Health.Builder();
            try (Connection conn = dataSource.getConnection()) {
                boolean valid = conn.isValid(5);
                if (valid) {
                    builder.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("catalog", conn.getCatalog());
                } else {
                    builder.down().withDetail("error", "Connection validation failed");
                }
            } catch (Exception e) {
                builder.down().withDetail("error", e.getMessage());
            }
            return builder.build();
        };
    }

    @Bean
    public HealthIndicator kafkaHealthIndicator() {
        return () -> Health.up()
                .withDetail("status", "configured")
                .withDetail("bootstrapServers", "kafka:29092")
                .build();
    }
}