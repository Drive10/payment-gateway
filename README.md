# PayFlow - Enterprise Payment Gateway

Cloud-native payment platform built with Spring Boot microservices, Kafka eventing, PostgreSQL, Redis, and a React checkout UI.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

## Current Scope (Active)

- `api-gateway` (`8080`): routing, auth validation, rate limiting, security headers
- `auth-service` (`8081`): registration/login, JWT, RBAC
- `order-service` (`8082`): order lifecycle, merchant-facing order flow
- `payment-service` (`8083`): payment orchestration, idempotency, webhook handling
- `notification-service` (`8084`): notifications and webhook delivery
- `simulator-service` (`8086`): simulated providers for local testing
- `web/payment-page` (`5173`): checkout frontend

## Planned / Optional Scope

- `analytics-service` (`8089`)
- `audit-service` (`8090`)
- `graphql-gateway` (`8087`)
- `search-service` (`8088`)

## Architecture

```text
Checkout UI -> API Gateway -> Auth / Order / Payment / Notification / Simulator
                               |
                               +-> Kafka events (payment.*, order.*, webhook.updates)
                               |
                               +-> PostgreSQL + Redis

Observability: Prometheus + Loki + Grafana + Zipkin
```

## Payment Flow Reality

- Payment responses are not final truth.
- Status progresses asynchronously: `PENDING -> webhook/event -> CAPTURED|FAILED`.
- Idempotency key is required for create/refund/retry paths.

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker + Docker Compose
- Node.js 20+

### 1) Start full stack in Docker

```bash
docker compose --profile infra --profile services up -d --build
```

### 2) Open key endpoints

- Checkout UI: `http://localhost:5173`
- API Gateway health: `http://localhost:8080/actuator/health`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki API: `http://localhost:3100`

### 3) Local frontend-only development

```bash
cd web/payment-page
npm ci
npm run dev
```

## Core API Examples

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflow.dev","password":"Demo@1234","firstName":"Demo","lastName":"User"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflow.dev","password":"Demo@1234"}'

# Create order (replace <token>)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"currency":"USD","customerEmail":"test@example.com","description":"Test Order"}'

# Create payment with idempotency key
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: demo-idem-001" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<order-id>","provider":"RAZORPAY_SIMULATOR","method":"CARD"}'
```

## Project Structure

```text
payflow/
|- libs/common/
|- services/
|  |- api-gateway/
|  |- auth-service/
|  |- order-service/
|  |- payment-service/
|  |- notification-service/
|  |- simulator-service/
|  |- analytics-service/     # optional/planned
|  \- audit-service/         # optional/planned
|- web/
|  \- payment-page/
|- infra/
|- docs/
\- docker-compose.yml
```

## Security and Configuration

- No production secrets should be committed.
- Use environment variables (`JWT_SECRET`, `GATEWAY_INTERNAL_SECRET`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`).
- See `SECURITY.md` and `.github/example-configs/.env.example`.

## Developer Docs

- `DEVELOPER_GUIDE.md` - local setup and troubleshooting
- `docs/SERVICES.md` - service-by-service reference
- `docs/INFRASTRUCTURE.md` - infra and observability details
- `docs/API.md` - API overview

## License

MIT - see `LICENSE`.
