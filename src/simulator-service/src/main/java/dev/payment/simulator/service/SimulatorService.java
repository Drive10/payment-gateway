package dev.payment.simulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class SimulatorService {
    private final Random random = new Random();
    private static final String[] OUTCOMES = {"CAPTURED", "AUTHORIZED", "CHALLENGE_REQUIRED", "FAILED"};
    private static final String[] FAILURE_REASONS = {
        "Insufficient funds",
        "Card declined",
        "Invalid card",
        "Expired card",
        "Processing error"
    };

    @KafkaListener(topics = "payment-events", groupId = "simulator-group")
    public void processPaymentEvent(String paymentId) {
        log.info("Processing payment event for: {}", paymentId);

        try {
            Thread.sleep(500 + random.nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String outcome = OUTCOMES[random.nextInt(OUTCOMES.length)];
        String failureReason = null;

        if ("FAILED".equals(outcome)) {
            failureReason = FAILURE_REASONS[random.nextInt(FAILURE_REASONS.length)];
        }

        log.info("Payment {} -> {} (failure: {})", paymentId, outcome, failureReason);
    }

    public String simulatePayment(String paymentId) {
        try {
            Thread.sleep(1000 + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (random.nextInt(10) < 8) {
            return "CAPTURED";
        } else if (random.nextInt(10) < 5) {
            return "FAILED";
        } else {
            return "CHALLENGE_REQUIRED";
        }
    }
}