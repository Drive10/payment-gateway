package dev.payment.paymentservice;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.service.PaymentEventPublisher;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@TestConfiguration
public class TestSupportConfig {

    @Bean
    @Primary
    PaymentEventPublisher paymentEventPublisher() {
        return new PaymentEventPublisher(null, "test-topic") {
            @Override
            public void publish(String eventType, Payment payment, Map<String, String> metadata) {
            }
        };
    }
}
