package dev.payment.audit.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String AUDIT_EVENTS_TOPIC = "payment-audit-events";

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(AUDIT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
