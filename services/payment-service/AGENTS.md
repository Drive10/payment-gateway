# Payment Service - Agent Rules

> Specific guidelines for AI agents working on the payment-service

---

## Service Overview

- **Port**: 8083
- **Database**: PostgreSQL
- **Events**: Kafka (`payment.created`, `payment.completed`, `payment.failed`, `payment.refunded`)
- **Dependencies**: Redis (idempotency), Stripe/Razorpay providers

---

## Key Responsibilities

1. Payment orchestration and processing
2. Multi-provider integration (Stripe, Razorpay, PayPal)
3. Idempotency handling
4. Webhook processing

---

## Important Files

```
services/payment-service/
├── src/main/java/com/payflow/payment/
│   ├── controller/PaymentController.java
│   ├── service/PaymentService.java
│   ├── provider/StripeProvider.java, RazorpayProvider.java
│   ├── repository/PaymentRepository.java
│   └── dto/PaymentRequest.java, PaymentResponse.java
└── src/main/resources/application.yml
```

---

## Critical Rules

1. **NEVER store raw card numbers** - Use tokens only
2. **Always implement idempotency** - Use Redis with 24h TTL
3. **Always use circuit breaker** - Resilience4j for provider calls
4. **Log masked card data only** - Last 4 digits only

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/payments | Create payment |
| GET | /api/v1/payments/{id} | Get payment |
| POST | /api/v1/payments/{id}/capture | Capture authorized |
| POST | /api/v1/payments/{id}/refunds | Refund payment |
| POST | /api/v1/payments/webhooks/{provider} | Webhook handler |

---

## Testing

```bash
# Run payment service tests
mvn test -pl services/payment-service

# Test specific payment flow
mvn test -Dtest=PaymentServiceTest -pl services/payment-service
```

---

## Related Docs

- [.ai/rules/payment.md](../../.ai/rules/payment.md) - Payment rules
- [.ai/rules/backend.md](../../.ai/rules/backend.md) - Backend rules
- [.ai/context/services.md](../../.ai/context/services.md) - Service context