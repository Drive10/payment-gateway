package dev.payment.common.util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Utility to wait for dependent services to be ready before starting Spring Boot application.
 * This prevents race conditions where services start before databases are fully ready.
 */
@Component
public class ServiceWaiter implements EnvironmentPostProcessor {

    private static final int MAX_ATTEMPTS = 30;
    private static final Duration SLEEP_DURATION = Duration.ofSeconds(2);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Wait for database to be ready
        waitForDatabase(environment);
        
        // Wait for Redis to be ready
        waitForRedis(environment);
        
        // Wait for Kafka to be ready
        waitForKafka(environment);
    }

    private void waitForDatabase(ConfigurableEnvironment environment) {
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        if (url != null && url.contains("postgres")) {
            // Extract host and port from JDBC URL
            // Format: jdbc:postgresql://host:port/database
            String[] parts = url.split("[:/]");
            String host = parts.length > 3 ? parts[3] : "postgres";
            int port = parts.length > 4 ? Integer.parseInt(parts[4]) : 5432;
            
            waitForHost(host, port, "PostgreSQL");
        }
    }

    private void waitForRedis(ConfigurableEnvironment environment) {
        String host = environment.getProperty("SPRING_DATA_REDIS_HOST", "redis");
        String portStr = environment.getProperty("SPRING_DATA_REDIS_PORT", "6379");
        int port = Integer.parseInt(portStr);
        
        waitForHost(host, port, "Redis");
    }

    private void waitForKafka(ConfigurableEnvironment environment) {
        String bootstrapServers = environment.getProperty("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
        String[] parts = bootstrapServers.split(":");
        String host = parts.length > 0 ? parts[0] : "kafka";
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9092;
        
        waitForHost(host, port, "Kafka");
    }

    private void waitForHost(String host, int port, String serviceName) {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    System.err.println("✗ Failed to connect to " + serviceName + " after " + MAX_ATTEMPTS + " attempts");
                    throw new IllegalStateException(serviceName + " is not available", e);
                }
                try {
                    Thread.sleep(SLEEP_DURATION.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for " + serviceName, ie);
                }
            }
        }
    }
}