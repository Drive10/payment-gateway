# PayFlow — Development Guide

## Quick Start

```bash
# Everything in Docker (11 containers, ~30s)
docker compose --profile services up -d

# Or just infra for local Maven dev
docker compose --profile infra up -d
```

## Test Accounts (auto-seeded)

| Email | Role | Password |
|-------|------|----------|
| `admin@payflow.dev` | Admin | `Password123` |
| `merchant@test.com` | Merchant | `Password123` |
| `dev@test.com` | Customer | `Password123` |

## Service Ports

| Service | Port | Access |
|---------|------|--------|
| Frontend | 5173 | Public (nginx → API gateway) |
| API Gateway | 8080 | Public |
| Auth | 8082 | Internal |
| Payment | 8083 | Internal |
| Notification | 8085 | Internal |
| Simulator | 8086 | Internal |
| Analytics | 8087 | Internal |
| Audit | 8088 | Internal |

## Testing the API

```bash
TOKEN=$(curl -s http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@test.com","password":"Password123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('accessToken',''))")

# Create + process + capture
PID=$(curl -s http://localhost:8080/api/v1/payments/create-order \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"orderId":"test-1","amount":5000,"currency":"INR","paymentMethod":"CARD"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('paymentId',''))")

curl -s -X POST "http://localhost:8080/api/v1/payments/$PID/process" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"cardNumber":"4111111111111111","expiry":"12/28","cvv":"123"}'

curl -s -X POST "http://localhost:8080/api/v1/payments/$PID/verify-otp" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"otp":"123456"}'

curl -s -X POST "http://localhost:8080/api/v1/payments/$PID/capture" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN"

curl -s "http://localhost:8080/api/v1/payments/$PID/status" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

## Event-Driven Architecture

```
Payment → OutboxPoller(5s) → Kafka → Simulator → Webhook → Payment Service
                                            → Analytics
                                            → Audit
                                            → Notification
```

## Docker Compose Profiles

```bash
# Infra only
docker compose --profile infra up -d

# All services
docker compose --profile services up -d

# Stop + wipe data
docker compose --profile services down --volumes
```

## Build

```bash
mvn clean package -DskipTests          # All JARs
docker compose --profile services build
docker compose --profile services up -d
```

## Key Files

| File | Purpose |
|------|---------|
| `docker-compose.yml` | 2 profiles: infra, services. YAML anchors for DRY |
| `src/*/application-docker.yml` | Docker Spring profile configs |
| `frontend/payment-page/src/pages/Processing.tsx` | Payment processing UX (card, UPI, netbanking, wallet) |
| `frontend/payment-page/src/components/DevTestPanel.tsx` | Sandbox dev tools |
