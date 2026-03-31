package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.IdempotencyRecord;
import dev.payment.paymentservice.domain.enums.IdempotencyRecordStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@EnableAutoConfiguration(exclude = KafkaAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Disabled("Requires full application context - run with integration tests")
class IdempotencyRecordRepositoryTest {

    @Autowired
    private IdempotencyRecordRepository repository;

    @Test
    void shouldEnforceUniqueIdempotencyKeyPerOperationAndActor() {
        repository.saveAndFlush(record(7L, "PAYMENT_CREATE", "idem-2001"));

        assertThatThrownBy(() -> repository.saveAndFlush(record(7L, "PAYMENT_CREATE", "idem-2001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameIdempotencyKeyForDifferentActors() {
        repository.saveAndFlush(record(7L, "PAYMENT_CREATE", "idem-2002"));
        repository.saveAndFlush(record(8L, "PAYMENT_CREATE", "idem-2002"));

        assertThat(repository.count()).isEqualTo(2);
    }

    private IdempotencyRecord record(Long actorId, String operation, String key) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setActorId(actorId);
        record.setOperation(operation);
        record.setIdempotencyKey(key);
        record.setRequestHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        record.setStatus(IdempotencyRecordStatus.COMPLETED);
        record.setResponsePayload("{\"ok\":true}");
        return record;
    }
}
