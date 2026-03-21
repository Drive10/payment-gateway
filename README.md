# Fintech Payment Platform

![Java](https://img.shields.io/badge/Java-21-007396?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-111827)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture%20%2B%20DDD-0F766E)
![OpenAPI](https://img.shields.io/badge/API-OpenAPI%203-85EA2D)

Production-style fintech backend built with Java 21 and Spring Boot 3.3. The repository is structured like a real payments team codebase: a gateway-ready edge module, a hardened payment domain core, bounded platform services, PostgreSQL migrations, JWT security, pagination and filtering, idempotent payment APIs, auditability, and containerized local runtime.

## Why It Looks Enterprise

- Clean separation into `controller`, `service`, `domain`, `repository`, `config`, `security`, `dto`, and `exception`.
- Domain-driven aggregates for users, roles, orders, payments, transactions, and audit logs.
- JWT authentication with role-based access for `ADMIN` and `USER`.
- Razorpay-style order and payment capture simulation with idempotency protection.
- Flyway-driven PostgreSQL schema with constraints, indexes, and seed data.
- Standard API envelope, global exception handling, validation, structured logging, and OpenAPI docs.
- API Gateway module wired to front the payment core and expose supporting service boundaries.

## Repository Layout

```text
payment-gateway
├── Dockerfile
├── docker-compose.yml
├── libs
│   └── common
├── services
│   ├── api-gateway
│   ├── auth-service
│   ├── ledger-service
│   ├── notification-service
│   ├── payment-service
│   ├── risk-service
│   └── settlement-service
└── observability
```

`payment-service` is the production-grade transactional core. The surrounding services now expose clear bounded-context starter endpoints so the repository presents a complete multi-service platform shape.

## Architecture

```text
                         +--------------------+
Client / Partner Apps -->| API Gateway        |
                         | Route / Edge Ready |
                         +---------+----------+
                                   |
                                   v
                +------------------+-------------------------------+
                |                  |                 |             |
                v                  v                 v             v
         +-------------+    +-------------+   +-----------+   +-------------+
         | auth svc    |    | payment svc |   | risk svc  |   | notify svc  |
         +-------------+    +-------------+   +-----------+   +-------------+
                                   |
                                   v
                          +------------------+
                          | ledger / settle  |
                          +------------------+
                                   |
                                   v
                             PostgreSQL 16
```

## Domain Flow

1. `USER` registers or logs in to receive a JWT.
2. Authenticated user creates an order.
3. User starts a payment with `Idempotency-Key`.
4. Service creates a simulated provider order id, marks the order as `PAYMENT_PENDING`, and records a transaction.
5. User captures the payment with provider payment metadata.
6. Service marks payment `CAPTURED`, order `PAID`, and appends an audit log.
7. `ADMIN` users can inspect all orders, payments, and audit logs.

Supporting platform service status routes are also available through the gateway:

- `GET /platform/auth/status`
- `GET /platform/ledger/status`
- `GET /platform/notification/status`
- `GET /platform/risk/status`
- `GET /platform/settlement/status`

## Core APIs

### Authentication

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Orders

- `POST /api/v1/orders`
- `GET /api/v1/orders?page=0&size=10&status=CREATED`

### Payments

- `POST /api/v1/payments`
- `POST /api/v1/payments/{paymentId}/capture`
- `GET /api/v1/payments?page=0&size=10&status=CAPTURED`
- `GET /api/v1/payments/{paymentId}`

### Admin

- `GET /api/v1/admin/orders`
- `GET /api/v1/admin/payments`
- `GET /api/v1/admin/audit-logs`

Swagger UI is available at [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html).

## Quick Start

### Local with Docker

```bash
cp .env.example .env
docker compose up --build
```

### Local with Maven

```bash
mvn -q -pl services/payment-service -am spring-boot:run
```

Open:

- Gateway: [http://localhost:8080](http://localhost:8080)
- API docs: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- Health: [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)

## Seed Credentials

- Admin email: `admin@fintech.local`
- Admin password: `Admin@1234`

## Example Flow

### 1. Register

```bash
curl -X POST http://localhost:8084/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName":"Rahul Sharma",
    "email":"rahul@example.com",
    "password":"User@1234"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"rahul@example.com",
    "password":"User@1234"
  }'
```

### 3. Create Order

```bash
curl -X POST http://localhost:8084/api/v1/orders \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "externalReference":"checkout-1001",
    "amount":2499.00,
    "currency":"INR",
    "description":"Premium plan subscription"
  }'
```

### 4. Initiate Payment

```bash
curl -X POST http://localhost:8084/api/v1/payments \
  -H "Authorization: Bearer <JWT>" \
  -H "Idempotency-Key: pay-checkout-1001" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":"<ORDER_ID>",
    "method":"UPI",
    "provider":"razorpay_simulator",
    "notes":"Customer selected UPI collect"
  }'
```

### 5. Capture Payment

```bash
curl -X POST http://localhost:8084/api/v1/payments/<PAYMENT_ID>/capture \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "providerPaymentId":"pay_sim_001",
    "providerSignature":"simulated_signature_hash"
  }'
```

## Engineering Notes

- Database migrations live in `services/payment-service/src/main/resources/db/migration`.
- Seed SQL also exists in `services/payment-service/src/main/resources/db/seed/seed-data.sql`.
- Shared API wrapper contracts live in `libs/common`.
- `services/api-gateway` includes route configuration to forward `/api/v1/**` to the payment service.
- Service architecture notes live in `docs/ARCHITECTURE.md`.
- GitHub Actions CI is defined in `.github/workflows/ci.yml`.

## What To Showcase On GitHub

- Clear layered package structure under `services/payment-service/src/main/java`.
- Fintech-specific schema and lifecycle modeling.
- Clean docs and architecture narrative.
- Production-friendly defaults without hard-coded local-only hacks.
