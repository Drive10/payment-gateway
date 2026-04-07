# Payment Rules

> Payment-specific standards and security requirements for PayFlow

---

## 1. PCI DSS Compliance

### Critical: Never Store Card Data

```java
// ❌ FORBIDDEN - Never do this
@Entity
class Payment {
    String cardNumber;  // NEVER
    String cvv;          // NEVER
    String expiryDate;  // NEVER
}

// ✅ CORRECT - Store only tokens
@Entity
class Payment {
    String paymentToken;  // From payment provider
    String lastFour;      // Only last 4 digits
    String cardBrand;     // VISA, MASTERCARD, etc.
}
```

### Tokenization Flow

```
1. User enters card details on frontend
2. Frontend sends directly to payment provider (Stripe/Razorpay)
3. Provider returns token
4. Token stored in our database (NEVER raw card data)
5. Use token for subsequent charges
```

---

## 2. Payment Providers

### Supported Providers

| Provider | Features | Config |
|----------|----------|--------|
| Stripe | Cards, Wallets, ACH | `STRIPE_API_KEY` |
| Razorpay | Cards, UPI, Wallets | `RAZORPAY_KEY_ID` |
| PayPal | PayPal, Cards | `PAYPAL_CLIENT_ID` |

### Provider Interface

```java
public interface PaymentProvider {
    PaymentResult charge(PaymentRequest request);
    PaymentResult refund(String transactionId, BigDecimal amount);
    PaymentResult getStatus(String transactionId);
}

@Service
public class StripeProvider implements PaymentProvider {
    @Override
    public PaymentResult charge(PaymentRequest request) {
        // Implement Stripe charges
    }
}
```

---

## 3. Idempotency

### Why Idempotency Matters

- Network failures can cause duplicate charges
- User refresh triggers retry
- Payment provider timeouts

### Implementation

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final RedisTemplate<String, String> redis;
    
    @Transactional
    public PaymentResult process(PaymentRequest request) {
        // Check idempotency key
        String idempotencyKey = request.getIdempotencyKey();
        String existing = redis.opsForValue().get("idempotency:" + idempotencyKey);
        
        if (existing != null) {
            // Return cached result - don't process again
            return objectMapper.readValue(existing, PaymentResult.class);
        }
        
        // Process payment
        PaymentResult result = provider.charge(request);
        
        // Store result with idempotency key
        redis.opsForValue().set(
            "idempotency:" + idempotencyKey,
            objectMapper.writeValueAsString(result),
            Duration.ofHours(24)
        );
        
        return result;
    }
}
```

### Idempotency Key Requirements

- Unique per merchant + payment combination
- Maximum 24-hour lifetime
- Include in request header: `Idempotency-Key`

---

## 4. Payment States

### State Machine

```
CREATED → PROCESSING → AUTHORIZED → CAPTURED
                    ↓
                 FAILED

AUTHORIZED → CAPTURED → REFUNDED
                    ↓
                PARTIALLY_REFUNDED
```

### State Enum

```java
public enum PaymentStatus {
    CREATED,        // Payment created, not yet processed
    PROCESSING,     // Currently being processed
    AUTHORIZED,     // Authorized but not captured
    CAPTURED,       // Successfully captured
    FAILED,         // Payment failed
    REFUNDED,       // Fully refunded
    PARTIALLY_REFUNDED  // Partial refund
}
```

---

## 5. Webhooks

### Webhook Handling

```java
@PostMapping("/webhooks/{provider}")
public ResponseEntity<Void> handleWebhook(
        @PathVariable String provider,
        @RequestBody String payload,
        @RequestHeader("X-Signature") String signature) {
    
    // Verify signature
    if (!verifySignature(provider, payload, signature)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    
    // Parse event
    WebhookEvent event = objectMapper.readValue(payload, WebhookEvent.class);
    
    // Process asynchronously
    kafkaTemplate.send("webhook.updates", event);
    
    return ResponseEntity.ok().build();
}
```

### Webhook Events

| Event | Description |
|-------|-------------|
| payment_intent.succeeded | Payment completed |
| payment_intent.failed | Payment failed |
| charge.refunded | Payment refunded |
| customer.created | New customer |

---

## 6. Refunds

### Refund Flow

```java
@PostMapping("/payments/{id}/refunds")
public ResponseEntity<RefundResponse> refund(
        @PathVariable UUID id,
        @RequestBody @Valid RefundRequest request) {
    
    Payment payment = paymentRepository.findById(id)
        .orElseThrow(() -> new PaymentNotFoundException(id));
    
    // Validate refund amount
    if (request.getAmount().compareTo(payment.getAmount()) > 0) {
        throw new InvalidRefundException("Exceeds payment amount");
    }
    
    // Process refund via provider
    RefundResult result = provider.refund(payment.getTransactionId(), request.getAmount());
    
    // Update payment status
    payment.setStatus(determineRefundStatus(payment, request.getAmount()));
    paymentRepository.save(payment);
    
    // Publish event
    kafkaTemplate.send("payment.refunded", payment.getId(), event);
    
    return ResponseEntity.ok(RefundResponse.from(result));
}
```

---

## 7. Error Codes

### Payment Errors

| Code | HTTP | Description |
|------|------|-------------|
| PAYMENT_DECLINED | 400 | Card declined by provider |
| INVALID_CARD | 400 | Invalid card details |
| EXPIRED_CARD | 400 | Card expired |
| INSUFFICIENT_FUNDS | 400 | Not enough funds |
| PROVIDER_TIMEOUT | 504 | Provider timeout |
| PROVIDER_UNAVAILABLE | 503 | Provider down |
| DUPLICATE_PAYMENT | 409 | Idempotency conflict |
| AMOUNT_MISMATCH | 400 | Amount doesn't match |

---

## Quick Reference

| Area | Rule |
|------|------|
| Card Data | NEVER store raw card numbers |
| Idempotency | Required for all payments |
| State Machine | Follow defined states |
| Webhooks | Verify signature, async processing |
| Refunds | Validate amount against payment |
| Errors | Use standard error codes |