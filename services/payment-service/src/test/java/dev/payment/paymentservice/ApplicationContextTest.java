package dev.payment.paymentservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(ApplicationContextTest.NoOpEventPublisherConfig.class)
class ApplicationContextTest {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class NoOpEventPublisherConfig {

        @Bean
        @Primary
        dev.payment.paymentservice.service.PaymentEventPublisher paymentEventPublisher() {
            return new dev.payment.paymentservice.service.PaymentEventPublisher(null, "test-topic") {
                @Override
                public void publish(String eventType, dev.payment.paymentservice.domain.Payment payment, java.util.Map<String, String> metadata) {
                }
            };
        }
    }
}
