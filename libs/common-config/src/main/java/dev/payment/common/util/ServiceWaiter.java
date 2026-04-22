package dev.payment.common.util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

@Component
public class ServiceWaiter implements EnvironmentPostProcessor {

    private static final int MAX_ATTEMPTS = 30;
    private static final Duration SLEEP_DURATION = Duration.ofSeconds(2);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        waitForDatabase(environment);
        waitForRedis(environment);
        waitForKafka(environment);
    }

    private void waitForDatabase(ConfigurableEnvironment environment) {
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        if (url != null && url.contains("postgres")) {
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