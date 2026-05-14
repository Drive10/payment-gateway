# PayFlow — Development Guide

## Quick Start (30 seconds)

```bash
# Prerequisites: Docker + Java 21 + Node 20
./start-dev.sh
```

This single command:
1. Creates `.env` with dev defaults (if missing)
2. Starts PostgreSQL, Redis, Kafka via Docker
3. Builds all 7 microservices
4. Starts them with `local` profile
5. Installs and starts the frontend
6. Seeds test data (3 users, 5 sample payments)

## Manual Start

```bash
# 1. Environment
cp .env.example .env           # Or edit manually

# 2. Infrastructure
docker compose up -d

# 3. Build
mvn clean package -DskipTests

# 4. Run services
./start-jars.sh                # From JARs (faster)
# OR
./start-services.sh            # From source

# 5. Frontend
cd frontend/payment-page && npm run dev
```

## Test Accounts

| Email | Role | Password |
|-------|------|----------|
| `admin@payflow.dev` | Admin | `Password123` |
| `merchant@test.com` | Merchant | `Password123` |
| `dev@test.com` | Customer | `Password123` |

All seeded on first startup via `DevDataInitializer`.

## Sample Payments (pre-seeded)

| Order | Amount | Status |
|-------|--------|--------|
| ORD-DEMO-001 | ₹5,000 | CAPTURED |
| ORD-DEMO-002 | $1,200 | CAPTURED |
| ORD-DEMO-003 | ₹2,500 | AUTHORIZED |
| ORD-DEMO-004 | €800 | FAILED |
| ORD-DEMO-005 | ₹9,999 | CREATED |

Seeded on first startup via `PaymentDevSeeder`.

## Service Ports

| Service | Port | Access |
|---------|------|--------|
| Frontend | 5173 | Public |
| API Gateway | 8080 | Public |
| Auth | 8082 | Internal |
| Payment | 8083 | Internal |
| Notification | 8085 | Internal |
| Simulator | 8086 | Internal |
| Analytics | 8087 | Internal |
| Audit | 8088 | Internal |

## Testing the API

```bash
# Login
TOKEN=$(curl -s http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","password":"Password123"}' | \
  grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# List payments (includes seeded sample data)
curl -s http://localhost:8080/api/payments/list \
  -H "Authorization: Bearer $TOKEN"

# Create + process + capture
PAYMENT_ID=$(curl -s http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"orderId":"test-1","amount":5000,"currency":"INR","paymentMethod":"CARD"}' | \
  grep -o '"paymentId":"[^"]*"' | cut -d'"' -f4)

curl -s -X POST "http://localhost:8080/api/payments/$PAYMENT_ID/process" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"cardNumber":"4111111111111111","expiry":"12/28","cvv":"123"}'

curl -s -X POST "http://localhost:8080/api/payments/$PAYMENT_ID/authorize" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"otp":"123456"}'

curl -s -X POST "http://localhost:8080/api/payments/$PAYMENT_ID/capture" \
  -H "Authorization: Bearer $TOKEN"
```

## Event-Driven Architecture

```
Payment → OutboxPoller(5s) → Kafka → Simulator → Webhook → Payment Service
                                            → Analytics (metrics)
                                            → Audit (compliance log)
                                            → Notification (webhook delivery)
```

## Makefile

```bash
make infra-up     # Docker: postgres + redis + kafka
make infra-down   # Stop infrastructure
make build        # Build all JARs
make test         # Run backend tests
make frontend     # Start frontend
make all-services # Start all microservices
make dev          # Full environment
make dev-lite     # Infra + payment + frontend
```
