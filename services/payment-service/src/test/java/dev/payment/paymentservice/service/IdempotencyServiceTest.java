package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.domain.IdempotencyRecord;
import dev.payment.paymentservice.domain.enums.IdempotencyRecordStatus;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    void shouldStartNewIdempotentRequestWhenNoRecordExists() {
        when(repository.findByOperationAndActorIdAndIdempotencyKey("PAYMENT_CREATE", 42L, "idem-1001"))
                .thenReturn(java.util.Optional.empty());
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyService.IdempotencyResult<SampleResponse> result = service.begin(
                "PAYMENT_CREATE",
                "idem-1001",
                42L,
                new RequestFingerprint("order-1001", "1499.00"),
                SampleResponse.class
        );

        assertThat(result.replayed()).isFalse();
        assertThat(result.cachedResponse()).isNull();
        assertThat(result.record().getStatus()).isEqualTo(IdempotencyRecordStatus.IN_PROGRESS);
        assertThat(result.record().getActorId()).isEqualTo(42L);
        assertThat(result.record().getIdempotencyKey()).isEqualTo("idem-1001");
    }

    @Test
    void shouldReplayStoredResponseForMatchingCompletedRequest() {
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setOperation("PAYMENT_CREATE");
        existing.setActorId(42L);
        existing.setIdempotencyKey("idem-1002");
        existing.setRequestHash(hash(new RequestFingerprint("order-1002", "2499.00")));
        existing.setStatus(IdempotencyRecordStatus.COMPLETED);
        existing.setResponsePayload("""
                {"id":"pay_1002","status":"CREATED"}
                """);

        when(repository.findByOperationAndActorIdAndIdempotencyKey("PAYMENT_CREATE", 42L, "idem-1002"))
                .thenReturn(java.util.Optional.of(existing));

        IdempotencyService.IdempotencyResult<SampleResponse> result = service.begin(
                "PAYMENT_CREATE",
                "idem-1002",
                42L,
                new RequestFingerprint("order-1002", "2499.00"),
                SampleResponse.class
        );

        assertThat(result.replayed()).isTrue();
        assertThat(result.cachedResponse()).isEqualTo(new SampleResponse("pay_1002", "CREATED"));
    }

    @Test
    void shouldRejectSameKeyWhenRequestFingerprintChanges() {
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setOperation("PAYMENT_CREATE");
        existing.setActorId(42L);
        existing.setIdempotencyKey("idem-1003");
        existing.setRequestHash(hash(new RequestFingerprint("order-1003", "999.00")));
        existing.setStatus(IdempotencyRecordStatus.COMPLETED);
        existing.setResponsePayload("""
                {"id":"pay_1003","status":"CREATED"}
                """);

        when(repository.findByOperationAndActorIdAndIdempotencyKey("PAYMENT_CREATE", 42L, "idem-1003"))
                .thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> service.begin(
                "PAYMENT_CREATE",
                "idem-1003",
                42L,
                new RequestFingerprint("order-1003", "1299.00"),
                SampleResponse.class
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void shouldRejectReplayWhileMatchingRequestIsStillInProgress() {
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setOperation("PAYMENT_CREATE");
        existing.setActorId(42L);
        existing.setIdempotencyKey("idem-1004");
        existing.setRequestHash(hash(new RequestFingerprint("order-1004", "549.00")));
        existing.setStatus(IdempotencyRecordStatus.IN_PROGRESS);

        when(repository.findByOperationAndActorIdAndIdempotencyKey("PAYMENT_CREATE", 42L, "idem-1004"))
                .thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> service.begin(
                "PAYMENT_CREATE",
                "idem-1004",
                42L,
                new RequestFingerprint("order-1004", "549.00"),
                SampleResponse.class
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getCode()).isEqualTo("REQUEST_ALREADY_IN_PROGRESS"));
    }

    @Test
    void shouldPersistSerializedResponseWhenCompletingRequest() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.complete(record, new SampleResponse("pay_1005", "CAPTURED"), "pay_1005");

        assertThat(record.getStatus()).isEqualTo(IdempotencyRecordStatus.COMPLETED);
        assertThat(record.getResourceId()).isEqualTo("pay_1005");
        assertThat(record.getResponsePayload()).contains("CAPTURED");
    }

    private String hash(Object value) {
        try {
            byte[] payload = new ObjectMapper().writeValueAsBytes(value);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record RequestFingerprint(String orderId, String amount) {
    }

    private record SampleResponse(String id, String status) {
    }
}
