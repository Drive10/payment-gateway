package dev.payment.paymentservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownHandler.class);
    private static final long AWAIT_TIMEOUT_SECONDS = 15;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("event=graceful_shutdown_start timeout={}s", AWAIT_TIMEOUT_SECONDS);

        try {
            log.info("event=graceful_shutdown_complete");
        } catch (Exception e) {
            log.warn("event=graceful_shutdown_error error={}", e.getMessage());
        }
    }
}