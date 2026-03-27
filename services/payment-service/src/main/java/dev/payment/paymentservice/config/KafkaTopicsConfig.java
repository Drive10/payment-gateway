package dev.payment.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!test")
public class KafkaTopicsConfig {

    @Bean
    NewTopic paymentEventsTopic(@Value("${application.kafka.topic.payment-events}") String topicName) {
        return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
    }
}
