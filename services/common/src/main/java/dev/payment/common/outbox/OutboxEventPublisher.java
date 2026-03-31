package dev.payment.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${outbox.kafka.topic-prefix:payment}")
    private String topicPrefix;

    public OutboxEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OutboxEvent event) {
        String topic = topicPrefix + "." + event.getAggregateType().toLowerCase() + ".events";
        String key = event.getAggregateId();

        try {
            kafkaTemplate.send(topic, key, event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to Kafka: topic={}, key={}, error={}", 
                                    topic, key, ex.getMessage());
                        } else {
                            log.info("Published event to Kafka: topic={}, key={}, partition={}, offset={}", 
                                    topic, key, 
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing to Kafka: {}", e.getMessage());
            throw e;
        }
    }
}
