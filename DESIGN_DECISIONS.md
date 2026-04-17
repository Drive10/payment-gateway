# PayFlow Design Decisions

> Rationale for key architectural and implementation choices

---

## 1. Event-Driven Architecture with Kafka

### Decision
Use Apache Kafka as the primary messaging backbone for all inter-service communication.

### Rationale
- **Durability**: Messages persisted to disk, survives broker restarts
- **Replayability**: Consumers can replay events for debugging/recovery
- **Scalability**: Horizontal scaling via consumer groups
- **Decoupling**: Services don't need to know about each other directly

### Alternatives Considered
- **RabbitMQ**: Less durable, harder to replay
- **AWS SQS**: Vendor lock-in, no replay capability
- **Direct HTTP**: Tight coupling, no durability

---

## 2. Outbox Pattern for Event Publishing

### Decision
Use the transactional outbox pattern to ensure at-least-once delivery.

### Implementation
```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Service   │      │   Outbox    │      │   Kafka     │
│   writes    │ ───► │    Table    │ ───► │   Relay     │
│   + event   │      │             │      │  (poll)     │
└─────────────┘      └─────────────┘      └─────────────┘
```

### Rationale
- **Atomicity**: Event and data written in same transaction
- **No 2PC**: Avoids distributed transaction complexity
- **Auditability**: Outbox table serves as event log

### Trade-offs
- Eventual consistency (small delay ~1-2 seconds)
- Additional complexity in relay service

---

## 3. Idempotency via Redis + Database

### Decision
Implement idempotency at two levels:
1. **Request-level**: Redis with 24h TTL for duplicate detection
2. **State-level**: Database unique constraints for critical operations

### Rationale
- **Redis**: Fast, in-memory lookup for common case
- **Database**: Permanent record, survives Redis failures
- **TTL**: Prevents unbounded memory growth

### Implementation
```java
// Pseudo-code
IdempotencyResult result = idempotencyService.begin(key, actorId, metadata);
if (result.replayed()) {
    return result.cachedResponse();
}
// Process...
idempotencyService.complete(result, response, resourceId);
```

---

## 4. Payment State Machine

### Decision
Explicit state machine with allowed transitions.

### Rationale
- **Explicit**: Clear what transitions are allowed
- **Auditable**: Every transition logged
- **Prevents Invalid States**: Cannot skip states

### State Transition Rules
```
PENDING → CREATED → AUTHORIZATION_PENDING → AUTHORIZED → PROCESSING → CAPTURED
    ↓          ↓               ↓                 ↓             ↓
  FAILED    FAILED           FAILED            FAILED       FAILED
                                                              ↓
                                                         REFUNDED
```

---

## 5. Webhook Processing

### Decision
1. Signature verification before processing
2. Event deduplication via event ID
3. Idempotent processing within handler

### Rationale
- **Signature**: Ensures message authenticity
- **Deduplication**: Prevents replay attacks
- **Idempotent**: Safe to process same event multiple times

### Provider-Specific Handling

| Provider | Signature Method | Event ID Header |
|----------|-----------------|------------------|
| Stripe | HMAC-SHA256 | `Stripe-Signature` |
| Razorpay | HMAC-SHA256 | `X-Razorpay-Event-Id` |
| PayPal | Certificate | `PayPal-Transmission-Id` |

---

## 6. Circuit Breaker Configuration

### Decision
Use Resilience4j circuit breakers per payment provider.

### Configuration
```yaml
circuitbreaker:
  simulator:
    sliding-window-size: 10
    failure-rate-threshold: 50
    wait-duration-in-open-state: 30s
    permitted-calls-in-half-open: 3
```

### Rationale
- **Isolation**: One provider failure doesn't affect others
- **Fast Fail**: Open circuit immediately prevents cascade
- **Self-healing**: Half-open allows recovery

---

## 7. PostgreSQL for Payments

### Decision
Use PostgreSQL as primary data store for payments.

### Rationale
- **ACID**: Critical for financial transactions
- **JSONB**: Flexible metadata storage
- **Row-level locking**: Prevents race conditions
- **MVCC**: Good concurrent access

### Trade-offs
- Horizontal scaling requires sharding (future)
- Not ideal for high-volume event storage

---

## 8. JWT Authentication

### Decision
Use HS512-signed JWTs with short-lived access tokens (1h) and long-lived refresh tokens (30d).

### Token Structure
```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "roles": ["MERCHANT"],
  "token_type": "access",
  "iat": 1712486400,
  "exp": 1712490000
}
```

### Rationale
- **HS512**: Symmetric, fast, secure enough for internal use
- **Short-lived access**: Limits exposure if token leaks
- **Refresh rotation**: Forces periodic re-authentication

---

## 9. No Raw Card Storage

### Decision
Never store raw card numbers. Use tokenization.

### Rationale
- **PCI DSS**: Reduces compliance scope
- **Security**: Even DB breach doesn't expose cards
- **Tokenization**: Can use same token for multiple payments

### Implementation
- Card number → tokenized before storage
- Only last 4 digits stored
- Token sent to provider, never stored locally

---

## 10. Simulated Mode for Testing

### Decision
Built-in simulator service for testing without real providers.

### Rationale
- **Reliability**: Tests don't depend on external services
- **Speed**: No network latency
- **Determinism**: Can simulate specific scenarios

### Test Cards
```
4111111111111111 - Always succeeds
4000000000000002 - Always declines
4000000000009995 - Insufficient funds
```

---

## Future Considerations

### Potential Changes

1. **GraphQL Gateway**: May add for complex querying
2. **Sharding**: PostgreSQL sharding for scale
3. **Event Sourcing**: Consider for payment audit trail
4. **Multi-tenancy**: Add tenant isolation

### Not Planned

- Switching to MongoDB for payments (PostgreSQL is better for ACID)
- Direct database sync between services (Kafka is correct choice)
- Custom crypto (JWT libs are battle-tested)
