# Architecture Review And Target Design

## Current Topology (Post-Refactor)

### Services
- **api-gateway** (8080): Edge routing, JWT validation for merchant routes, pass-through for payments
- **merchant-backend** (8081): NEW - Trusted adapter, JWT auth from frontend, API key auth to payment service
- **payment-service** (8083): Payment lifecycle, idempotency, Kafka/outbox, simulator integration
- **notification-service**: Webhook and notification delivery
- **simulator-service**: Deterministic provider mock
- **analytics-service**: Risk and settlement analytics
- **audit-service**: Compliance audit logs

### Communication Pattern

```
Frontend (UNTRUSTED)
    │
    ▼ (JWT)
API Gateway
    │
    ├─► /api/v1/merchant/* ──► Merchant Backend (TRUSTED)
    │                              │
    │                              ▼ (API Key)
    │                         Payment Service
    │                              │
    │                              ▼
    │                         Simulator
    │
    └─► /api/v1/payments/* ──► Payment Service
         (pass-through, no JWT validation)
```

## Data Ownership

- **Payment domain**: `payments`, `transactions`, `refunds`, `idempotency_records`, `payment_outbox_events`, `ledger_*`
- **Notification domain**: webhook delivery states and channel dispatch history
- **Audit domain**: compliance and security audit logs
- **Analytics domain**: derived metrics and risk/settlement projections

## Sync vs Async Communication

### Sync
- Client -> Gateway -> Merchant Backend -> Payment for intent/capture/refund APIs
- Payment -> Simulator for provider intent/capture simulation

### Async
- Payment outbox -> Kafka payment events
- Webhook updates consumed by payment listener for async status reconciliation
- Notification and analytics consumers subscribe to payment events

## Transaction Boundaries

- ACID boundary: per-service database transaction
- Cross-service consistency: event-based eventual consistency using outbox and retry
- Failure strategy: retry with exponential backoff, then DLQ with audit event for manual intervention

## Trust Boundary Model

| Route | Authentication | Consumer |
|-------|---------------|----------|
| `/api/v1/merchant/*` | JWT (Bearer token) | Frontend → untrusted |
| `/api/v1/payments/*` | API Key (Bearer sk_test_*) | Merchant Backend → trusted |

## Security Enforcement

1. **Frontend cannot specify amounts** - Only sends `productId`
2. **merchantId not in request body** - Resolved from API key
3. **Order controlled by merchant-backend** - Not client-side
4. **API keys in payment service** - Replaces JWT-based auth
5. **Idempotency mandatory** - Prevents duplicate charges

## Anti-Patterns Fixed

| Before | After |
|--------|-------|
| Frontend → /payments | Frontend → /merchant/create-payment |
| JWT for payment auth | API Key (sk_test_*) |
| merchantId in request | API key resolves merchant |
| Order created in payment service | Order snapshot from merchant |
| Frontend specifies amount | Server-controlled amount |

## Next Steps

1. **Merchant registration**: API key generation and management
2. **Webhook delivery**: Payment service → Merchant Backend → external systems
3. **Schema migrations**: Flyway/Liquibase for production
4. **Service identity**: mTLS for internal communication
5. **Event schema versioning**: Compatibility checks in CI