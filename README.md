# Fintech Payment Gateway

**Enterprise-grade microservices payment platform with Java 21, Spring Boot 3.3, Kafka, and React**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.8-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

---

## Quick Start

```bash
# One-command startup
git clone https://github.com/your-org/payment-gateway.git
cd payment-gateway
./scripts/start.sh
```

**Access Points:**
- API Gateway: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Grafana: http://localhost:3002 (admin/admin123)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL SYSTEMS                                     │
│  Card Networks │ Banks/ABKS │ Email/SMS │ Banking Partners                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API GATEWAY (8080)                                   │
│  JWT Auth │ Rate Limiting │ Security Headers │ Circuit Breaker              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
        ▼                            ▼                            ▼
┌──────────────┐            ┌──────────────┐            ┌──────────────┐
│Auth Service  │            │Order Service │            │Payment Svc  │
│   (8081)    │            │   (8082)    │            │   (8083)    │
└──────────────┘            └──────────────┘            └──────────────┘
        │                            │                            │
        └────────────────────────────┴────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KAFKA EVENT BUS                                          │
│  Topics: payment.events, refund.events, settlement.events, risk.events             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
        ▼                            ▼                            ▼
┌──────────────┐            ┌──────────────┐            ┌──────────────┐
│Settlement    │            │   Risk       │            │Analytics    │
│  (8087)     │            │   (8088)    │            │  (8089)     │
└──────────────┘            └──────────────┘            └──────────────┘
```

---

## Microservices (16 Services)

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | JWT validation, rate limiting, routing |
| **Config Service** | 8888 | Centralized configuration (Spring Cloud Config) |
| **Auth Service** | 8081 | User auth, JWT, API keys |
| **Order Service** | 8082 | Order management |
| **Payment Service** | 8083 | Payment processing, refunds |
| **Notification Service** | 8084 | Email/SMS notifications |
| **Webhook Service** | 8085 | Webhook delivery, retry |
| **Simulator Service** | 8086 | Payment provider simulator |
| **Settlement Service** | 8087 | Daily settlements, payouts |
| **Risk Service** | 8088 | Fraud detection |
| **Analytics Service** | 8089 | Metrics, reporting |
| **Merchant Service** | 8090 | Merchant onboarding, KYC |
| **Dispute Service** | 8091 | Chargeback management |
| **Feature Flags** | 8092 | Feature toggles |

---

## Key Features

### Payment Lifecycle
```
INITIATED → AUTHORIZED → CAPTURED → COMPLETED
                    ↓            ↓
                 FAILED      REFUNDED (partial/full)
```

### Event-Driven Architecture
- **Outbox Pattern** for reliable event publishing
- **Saga Pattern** for distributed transactions
- **Dead Letter Queue** for failed messages
- **Event versioning** for schema evolution

### Resilience
- **Circuit Breaker** (Resilience4j)
- **Retry with exponential backoff**
- **Timeout handling**
- **Bulkhead isolation**

### Security
- JWT authentication
- RBAC (Role-Based Access Control)
- HMAC webhook validation
- Rate limiting per user/API key
- Security headers

---

## System Flow

### Payment Flow (Happy Path)
```
1. Client → API Gateway (auth + rate limit)
2. Gateway → Auth Service (validate JWT)
3. Auth → Gateway (token valid)
4. Gateway → Payment Service (create payment)
5. Payment → Risk Service (fraud check)
6. Risk → Payment (approved/declined)
7. Payment → Simulator (process payment)
8. Simulator → Payment (success/failure)
9. Payment → Kafka (event published)
10. Payment → Client (response)
```

### Idempotency
- `Idempotency-Key` header ensures exactly-once processing
- Keys stored with 24h expiration
- Duplicate requests return cached response

---

## API Documentation

### Authentication
```bash
# Register
POST /api/v1/auth/register
{"email": "user@example.com", "password": "password123", "name": "User"}

# Login
POST /api/v1/auth/login
{"email": "user@example.com", "password": "password123"}
→ {"accessToken": "eyJ...", "refreshToken": "eyJ..."}
```

### Payments
```bash
# Create Payment
POST /api/v1/payments
Headers: Authorization: Bearer <token>, Idempotency-Key: <uuid>
{
  "amount": 5000,
  "currency": "USD",
  "paymentMethod": {
    "type": "card",
    "card": {"number": "4242...", "expiryMonth": "12", "expiryYear": "25", "cvv": "123"}
  }
}

# Capture
POST /api/v1/payments/{id}/capture

# Refund
POST /api/v1/payments/{id}/refunds
{"amount": 1000, "reason": "CUSTOMER_REQUEST"}
```

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_DECLINED",
    "message": "Card declined by issuer",
    "correlationId": "abc-123"
  }
}
```

---

## Observability

### Prometheus Metrics
```
# Payment success rate
payment_success_total / payment_total * 100

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# Circuit breaker state
resilience4j_circuitbreaker_state{name="payment"}
```

### Distributed Tracing (Jaeger)
- Every request tagged with `correlationId`
- Trace spans for: Gateway → Auth → Service → DB → Kafka
- End-to-end latency breakdown

### Centralized Logging (Loki)
```
# Query all payment errors
{app="payment-service"} |= "ERROR" |= "payment"

# Query by correlation ID
{app="payment-service"} |= "correlationId=abc-123"
```

---

## Design Decisions

### Why Kafka over REST for inter-service communication?

| Factor | REST | Kafka |
|--------|------|-------|
| **Coupling** | Tight (direct calls) | Loose (event-driven) |
| **Reliability** | Need circuit breakers | Built-in retry + DLQ |
| **Scalability** | Thread-per-request | Partitioned consumers |
| **Audit Trail** | None | Events stored permanently |
| **Use Case** | Synchronous queries | Async workflows |

**Decision:** Kafka for all async workflows (payments, settlements, notifications). REST for synchronous queries only.

### Why Outbox Pattern?

```
Problem: DB write + Kafka publish are not atomic
Solution: Single DB transaction writes both payment + outbox event
         Outbox processor polls and publishes to Kafka
```

### Why Database-per-Service?

- **Isolation:** Each service owns its data
- **Scalability:** Scale services independently
- **Technology:** Choose best DB per use case
- **Failure isolation:** DB outage doesn't cascade

---

## Failure Scenarios

### 1. Payment Service Crashes After DB Write
```
Solution: Outbox Pattern
- Payment + OutboxEvent written in same transaction
- On restart, outbox processor replays pending events
- At-least-once delivery, consumer handles idempotency
```

### 2. Kafka Consumer Fails
```
Solution: Retry with Exponential Backoff
- Attempt 1: Immediate
- Attempt 2: 2s
- Attempt 3: 4s
- Attempt 4: 8s
- Attempt 5: Move to DLQ + Alert
```

### 3. Idempotency Key Collision
```
Solution: Store idempotency keys in DB
- Check on every request
- If exists: return cached response
- Expire after 24h
```

---

## Scaling Strategy

### Stateless Services (Horizontal Scaling)
```
API Gateway ─┬─▶ Auth (3 pods)
             ├─▶ Payment (10 pods) ← hot path
             ├─▶ Order (5 pods)
             └─▶ Notification (2 pods)
```

### Database Scaling
```
Primary DB ← All writes + critical reads
     ↑
Read Replica ← Analytics, reporting
```

### Caching Strategy
- **Redis:** Sessions, rate limits, feature flags
- **Local:** Payment method lookup, merchant configs

---

## Project Structure

```
payment-gateway/
├── services/
│   ├── api-gateway/          # Edge routing
│   ├── auth-service/          # Authentication
│   ├── payment-service/       # Core payments
│   ├── order-service/         # Orders
│   ├── notification-service/  # Notifications
│   ├── webhook-service/       # Webhooks
│   ├── simulator-service/     # Testing
│   ├── settlement-service/    # Settlements
│   ├── risk-service/         # Fraud detection
│   ├── analytics-service/     # Metrics
│   ├── merchant-service/      # Merchants
│   ├── dispute-service/       # Disputes
│   ├── featureflags-service/  # Feature toggles
│   └── config-service/       # Config server
├── libs/
│   └── common/               # Shared code
├── docker/                   # Docker configs
├── docker-compose.yml         # Full stack
├── docs/                     # Documentation
│   ├── api/                 # OpenAPI spec
│   ├── demo/                # Demo guide
│   └── ARCHITECTURE.md      # Architecture
├── infrastructure/           # Terraform (AWS)
├── scripts/                  # Dev scripts
└── tests/                    # Load tests
```

---

## Contributing

```bash
# Setup
git clone https://github.com/your-org/payment-gateway.git
cd payment-gateway

# Build
mvn clean install

# Run tests
mvn test

# Start for development
docker compose --profile infra up -d
./mvnw spring-boot:run -pl services/api-gateway
```

---

## License

MIT License - see [LICENSE](LICENSE)

---

**Built with enterprise-grade patterns for production payment systems**
