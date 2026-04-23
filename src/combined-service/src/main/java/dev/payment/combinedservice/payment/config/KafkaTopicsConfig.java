package dev.payment.combinedservice.payment.config;

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
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentEventsDltTopic(@Value("${application.kafka.topic.payment-events-dlt}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentStatusUpdatedTopic(@Value("${application.kafka.topic.payment-status-updated}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic webhookUpdatesTopic(@Value("${application.kafka.topic.webhook-updates}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }
}
