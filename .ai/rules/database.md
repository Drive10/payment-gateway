# Database Rules

> Database standards for PayFlow

---

## 1. PostgreSQL Standards

### JPA Entities

```java
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_order_id", columnList = "orderId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    // Tokenized card data only - NEVER store actual card numbers
    @Column
    private String paymentToken;
    
    @Column(length = 4)
    private String lastFour;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

---

## 2. Migrations

### Flyway Migrations

- Location: `src/main/resources/db/migration/`
- Naming: `V1__description.sql`, `V2__description.sql`
- **Never modify existing migrations**
- Test migrations locally first

### Migration Example

```sql
-- V1__initial_schema.sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_token VARCHAR(255),
    last_four VARCHAR(4),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
```

---

## 3. MongoDB Standards

### Audit Collections

```java
@Document(collection = "audit_events")
public class AuditEvent {
    @Id
    private String id;
    
    @Field("event_type")
    private String eventType;
    
    @Field("entity_type")
    private String entityType;
    
    @Field("entity_id")
    private String entityId;
    
    @Field("user_id")
    private String userId;
    
    @Field("timestamp")
    private Instant timestamp;
    
    @Field("changes")
    private Map<String, Object> changes;
}
```

---

## 4. Redis Standards

### Cache Usage

```java
@Cacheable(value = "payments", key = "#id")
public Payment getPayment(String id) {
    return repository.findById(id).orElseThrow();
}

@CacheEvict(value = "payments", key = "#id")
public void updatePayment(Payment payment) {
    repository.save(payment);
}
```

### Rate Limiting

```lua
-- Lua script for rate limiting
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])

local current = redis.call('GET', key)
if current and tonumber(current) >= limit then
    return 0
end

local count = redis.call('INCR', key)
if count == 1 then
    redis.call('EXPIRE', key, window)
end

return count
```

---

## 5. Elasticsearch Standards

### Index Mapping

```json
{
  "mappings": {
    "properties": {
      "paymentId": { "type": "keyword" },
      "amount": { "type": "scaled_float", "scaling_factor": 100 },
      "currency": { "type": "keyword" },
      "status": { "type": "keyword" },
      "customerEmail": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "merchantId": { "type": "keyword" }
    }
  }
}
```

---

## 6. Query Optimization

### Avoid N+1 Queries

```java
// BAD - N+1 queries
List<Payment> payments = repository.findAll();
payments.forEach(p -> log.info(p.getCustomer().getEmail()));

// GOOD - JOIN fetch
@Query("SELECT p FROM Payment p LEFT JOIN FETCH p.customer WHERE p.status = :status")
List<Payment> findAllWithCustomer(@Param("status") PaymentStatus status);
```

### Use Projections

```java
public interface PaymentSummary {
    UUID getId();
    BigDecimal getAmount();
    String getStatus();
}
```

---

## Quick Reference

| Database | Use For | Key Rule |
|----------|---------|----------|
| PostgreSQL | Orders, Payments, Users | Use Flyway, never auto-DDL |
| MongoDB | Audit logs, Events | Flexible schemas |
| Redis | Cache, Rate limit | TTL for all keys |
| Elasticsearch | Search, Analytics | Index with proper mappings |