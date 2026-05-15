<div align="center">
  <img src="https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.5-%236DB33F?logo=springboot" alt="Spring Boot 3.2"/>
  <img src="https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript" alt="TypeScript"/>
  <img src="https://img.shields.io/badge/React-18-61DAFB?logo=react" alt="React 18"/>
  <img src="https://img.shields.io/badge/Apache_Kafka-231F20?logo=apachekafka" alt="Kafka"/>
  <img src="https://img.shields.io/badge/PostgreSQL-14-4169E1?logo=postgresql" alt="PostgreSQL 14"/>
  <img src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis" alt="Redis 7"/>
  <img src="https://img.shields.io/badge/Docker_Compose-2496ED?logo=docker" alt="Docker Compose"/>
  <br/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT License"/>
  <img src="https://img.shields.io/badge/coverage-80%25-brightgreen" alt="Coverage"/>
</div>

<h1 align="center">PayFlow</h1>
<p align="center"><b>Production-grade open-source payment gateway — 7 microservices, event-driven, multi-currency</b></p>

<p align="center">PayFlow models the internal architecture of Stripe, Razorpay, and PayPal at project scale: API Gateway → domain services → Kafka event bus → analytics/audit. Full payment lifecycle with idempotency, outbox pattern, webhooks, and reconciliation.</p>

---

## ✨ Features

| Capability | Details |
|------------|---------|
| **Full Payment Lifecycle** | Create → Confirm → Authorize → Capture → Refund → Settle |
| **Multi-Currency** | INR, USD, EUR, GBP with full/partial refunds |
| **3DS & OTP Flows** | Simulated 3D Secure challenge, OTP verification, failure scenarios |
| **Event-Driven** | Transactional outbox → Kafka → Simulator/Analytics/Audit/Notification |
| **Idempotency** | Atomic Redis `SET NX` + DB-level unique constraint — no duplicate charges |
| **State Machine** | Enforced payment status transitions (CREATED → AUTHORIZED → CAPTURED → REFUNDED) |
| **API Gateway** | JWT auth, rate limiting (Redis-based), CORS, correlation IDs |
| **Webhooks** | HMAC-signed provider callbacks with inbox deduplication |
| **DLQ & Retry** | Dead-letter queue with configurable retry and replay |
| **Double-Entry Ledger** | Journal + ledger entries with balance validation |
| **Settlement** | Batch settlement processing with fee calculation |
| **Reconciliation** | Automated drift detection between local and provider state |
| **Dev Tools** | Sandbox checkout with card scheme presets, outcome simulation, one-click payments |

## 🏗️ Architecture

```
                          ┌──────────────────┐
                          │  React Frontend   │
                          │   localhost:5173  │
                          └────────┬─────────┘
                                   │ /api/*
                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     API Gateway · localhost:8080                      │
│     JWT auth · rate limiting · CORS · correlation-id · routing      │
└──────┬──────────────────────────────┬───────────────────────────────┘
       │ /auth/*                       │ /api/payments/*
       ▼                               ▼
┌──────────────────┐       ┌──────────────────────────┐
│  Auth Service    │       │   Payment Service         │
│  localhost:8082  │       │   localhost:8083          │
│                  │       │                           │
│ • JWT tokens    │       │ • Payment lifecycle      │
│ • Merchants     │       │ • Idempotency             │
│ • API keys      │       │ • Refunds                 │
│ • Dev seeder    │       │ • Ledger                  │
└────────┬─────────┘       │ • Webhook processing    │
         │                 └───────────┬───────────────┘
         │                             │
         └──────────┬──────────────────┘
                    ▼
           ┌────────────────┐
           │   Kafka Bus    │
           │  payment.*     │
           │  webhook.*     │
           │  audit.*       │
           └───┬────┬────┬──┘
       ┌───────┘    │    └────────┐
       ▼            ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│Simulator │ │Analytics │ │Audit Service │
│:8086     │ │:8087     │ │:8088         │
│          │ │          │ │              │
│Mock      │ │Metrics   │ │Immutable log │
│provider  │ │Settlement│ │Compliance    │
└──────────┘ └──────────┘ └──────────────┘

· PostgreSQL · Redis · Kafka · Docker Compose
```

## 🚀 Quick Start

```bash
# One command — starts everything (11 containers)
docker compose --profile services up -d

# Or start infrastructure only (dev mode)
docker compose --profile infra up -d
```

All services are up in ~30 seconds. No `.env` file needed — defaults are built into the `docker` Spring profile.

### Verify It Works

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","password":"Password123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))")

# 2. Create a payment
PAYMENT_ID=$(curl -s -X POST http://localhost:8080/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"orderId":"test-1","amount":5000,"currency":"INR","paymentMethod":"CARD"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('paymentId',''))")

# 3. Process card
curl -s -X POST "http://localhost:8080/api/v1/payments/$PAYMENT_ID/process" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"cardNumber":"4111111111111111","expiry":"12/28","cvv":"123"}'

# 4. OTP verify
curl -s -X POST "http://localhost:8080/api/v1/payments/$PAYMENT_ID/verify-otp" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"otp":"123456"}'

# 5. Capture
curl -s -X POST "http://localhost:8080/api/v1/payments/$PAYMENT_ID/capture" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN"

# Check status
curl -s "http://localhost:8080/api/v1/payments/$PAYMENT_ID/status" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Frontend

Visit **http://localhost:5173** — use the Sandbox mode for the full checkout UX with OTP modal, 3DS challenge, and success/failure pages.

### Test Accounts (seeded automatically)

| Email | Role | Password |
|-------|------|----------|
| `admin@payflow.dev` | Admin | `Password123` |
| `merchant@test.com` | Merchant | `Password123` |
| `dev@test.com` | Customer | `Password123` |

## 🧭 Service Reference

| Service | Port | Profile | Dependencies | Health |
|---------|------|---------|-------------|--------|
| `postgres` | 5432 | infra | — | `pg_isready` |
| `redis` | 6379 | infra | — | `redis-cli ping` |
| `kafka` | 9092 | infra | — | broker API versions |
| `auth-service` | 8082 | services | postgres, redis | `/actuator/health` |
| `api-gateway` | 8080 | services | auth, redis | `/actuator/health` |
| `payment-service` | 8083 | services | postgres, redis, kafka | `/actuator/health` |
| `notification-service` | 8085 | services | kafka | `/actuator/health` |
| `simulator-service` | 8086 | services | kafka | `/actuator/health` |
| `analytics-service` | 8087 | services | postgres, kafka | `/actuator/health` |
| `audit-service` | 8088 | services | postgres, kafka | `/actuator/health` |
| `frontend` | 5173 | services | api-gateway | HTTP 200 |

### Docker Compose Profiles

```bash
# Infrastructure only
docker compose --profile infra up -d

# All services (includes infra)
docker compose --profile services up -d

# Stop everything
docker compose --profile services down

# Wipe all data
docker compose --profile services down --volumes
```

## 💳 Payment Lifecycle

```
CREATED → AUTHORIZATION_PENDING → CHALLENGE_REQUIRED → AUTHORIZED → CAPTURED → REFUNDED
                                     ↓                                       ↓
                                   FAILED                                 PARTIAL_REFUND
```

### Card Test Scenarios

| Card Number | Outcome | Test |
|------------|---------|------|
| `4111 1111 1111 1111` | **Success** | OTP → Authorize → Capture |
| `4000 0000 0000 0000` | **Failure** | Declined |
| `4002 0000 0000 0000` | **3DS** | 3D Secure challenge |
| `4003 0000 0000 0000` | **OTP** | OTP verification |
| `5111 1111 1111 1111` | **Mastercard** | Same as Visa success |

### Net Banking Test

```bash
# Use the frontend Dev Tools panel to test
# Click "NetBanking" tab → pick a bank → "Pay ₹2,500 via HDFC NetBanking"
```

### Wallet Test

```bash
# Use the frontend Dev Tools panel
# Click "Amounts" tab → "Pay ₹1,500 via PhonePe Wallet"
```

## 🔧 Development

### Local (Maven, no Docker for services)

```bash
# Start infra
docker compose --profile infra up -d

# Build everything
mvn clean package -DskipTests

# Run a specific service
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local

# Start frontend
cd frontend/payment-page && npm run dev
```

### Makefile

```bash
make infra-up       # Docker: postgres + redis + kafka
make infra-down     # Stop infrastructure
make build          # Build all JARs
make test           # Run backend tests
make frontend       # Start frontend dev server
make all-services   # Start all microservices (Maven)
make dev            # Full environment
make dev-lite       # Infra + payment + frontend
make test-payment-card    # Quick card flow test via curl
make test-payment-upi      # Quick UPI flow test via curl
```

### Build Docker Images

```bash
mvn clean package -DskipTests
docker compose --profile services build
docker compose --profile services up -d
```

## 📁 Repository Layout

```
payflow/
├── src/
│   ├── api-gateway/           # Spring Cloud Gateway
│   ├── auth-service/          # JWT + merchant auth
│   ├── payment-service/       # Core payment engine
│   ├── notification-service/  # Webhooks
│   ├── simulator-service/     # Mock payment provider
│   ├── analytics-service/     # Metrics & settlements
│   └── audit-service/         # Compliance log
├── frontend/
│   └── payment-page/          # React + TypeScript checkout
├── config/
│   ├── init-schemas.sql       # DB schema init
│   ├── k8s/                   # Kubernetes manifests
│   ├── helm/                  # Helm chart
│   └── monitoring/            # Prometheus + Grafana + Loki
├── docs/
│   └── runbook.md             # On-call runbook
├── docker-compose.yml         # Profiles: infra, services
├── Makefile
├── AGENTS.md                  # AI-assisted dev guide
└── .github/                   # CI, templates, CODEOWNERS
```

## 🧪 Testing

```bash
# Backend tests
mvn test

# Frontend tests
cd frontend/payment-page && npm test

# E2E tests (requires full stack running)
cd frontend/payment-page && npm run test:e2e

# Quick API smoke test
make test-payment-card
```

## 📊 Observability

- **Correlation IDs** flow through all services (header + MDC)
- **Micrometer metrics** for idempotency cache, outbox, ledger
- **Prometheus** endpoints at `/actuator/prometheus`
- **Grafana** dashboard at `config/monitoring/grafana/`
- **Structured JSON logs** with trace IDs
- **Health checks** at `/actuator/health`

## 🛡️ Security

| Layer | Mechanism |
|-------|-----------|
| API Gateway | JWT validation, rate limiting, CORS |
| Auth Service | HS384 JWT, refresh tokens, API key management |
| Payment Service | Internal service token, merchant API keys |
| Webhooks | HMAC-SHA256 signature with nonce replay protection |
| Database | Connection-level auth, schema isolation per service |
| Redis | Password-protected |
| Secrets | No hardcoded secrets — all via environment/defaults in docker profile |

### Error Codes

| Code | Meaning |
|------|---------|
| `VALIDATION_ERROR` | Request validation failed |
| `PAYMENT_NOT_FOUND` | Payment not found |
| `INVALID_STATE_TRANSITION` | Invalid payment status change |
| `INVALID_REQUEST` | Invalid parameters |
| `PAYMENT_FAILED` | Payment processing failed |
| `REFUND_FAILED` | Refund processing failed |
| `CURRENCY_NOT_SUPPORTED` | Unsupported currency |
| `INTERNAL_ERROR` | Internal server error |

## 📝 License

MIT — see [LICENSE](LICENSE).

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports and feature requests welcome via GitHub Issues.
