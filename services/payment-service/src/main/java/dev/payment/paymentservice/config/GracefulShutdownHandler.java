package dev.payment.paymentservice.config;

import dev.payment.paymentservice.service.PaymentMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownHandler.class);
    
    private final DataSource dataSource;
    private final PaymentMetricsService metricsService;
    private final long awaitTimeoutSeconds;
    
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    public GracefulShutdownHandler(
            DataSource dataSource,
            PaymentMetricsService metricsService,
            @Value("${spring.lifecycle.timeout-per-shutdown-phase:30s}") String lifecycleTimeout
    ) {
        this.dataSource = dataSource;
        this.metricsService = metricsService;
        this.awaitTimeoutSeconds = parseTimeout(lifecycleTimeout);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (!shutdownInitiated.compareAndSet(false, true)) {
            log.info("Graceful shutdown already in progress");
            return;
        }

        log.info("event=graceful_shutdown_start timeout={}s", awaitTimeoutSeconds);
        long startTime = System.currentTimeMillis();

        try {
            validateDatabaseConnections();
            
            log.info("event=graceful_shutdown_complete duration={}ms", 
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("event=graceful_shutdown_error error={}", e.getMessage(), e);
        }
    }

    private void validateDatabaseConnections() {
        try {
            log.info("event=validating_db_connections");
            
            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        log.warn("event=db_connection_invalid");
                    }
                }
            }
            
            log.info("event=db_connections_validated");
        } catch (Exception e) {
            log.warn("event=db_validation_error error={}", e.getMessage());
        }
    }

    private long parseTimeout(String timeout) {
        if (timeout == null) {
            return 30;
        }
        try {
            if (timeout.endsWith("s")) {
                return Long.parseLong(timeout.substring(0, timeout.length() - 1));
            }
            return Long.parseLong(timeout);
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated.get();
    }
}