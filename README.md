# Fintech Payment Platform

![Java](https://img.shields.io/badge/Java-21-007396?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-111827)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture%20%2B%20DDD-0F766E)
![OpenAPI](https://img.shields.io/badge/API-OpenAPI%203-85EA2D)

Production-style fintech platform built with Java 21 and Spring Boot 3.3. The repository is structured like a real payments team codebase: a gateway-ready edge module, a hardened payment domain core, bounded platform services, isolated service databases, JWT security, pagination and filtering, idempotent payment APIs, auditability, a Dockerized React checkout, and containerized local runtime.

## Why It Looks Enterprise

- Clean separation into `controller`, `service`, `domain`, `repository`, `config`, `security`, `dto`, and `exception`.
- Domain-driven aggregates for users, roles, orders, payments, transactions, and audit logs.
- JWT authentication with role-based access for `ADMIN` and `USER`.
- Razorpay-style order and payment capture simulation with idempotency protection.
- PostgreSQL-backed domain model with per-service database isolation and environment-specific startup behavior.
- Standard API envelope, global exception handling, validation, structured logging, and OpenAPI docs.
- API Gateway module wired to front the payment core and expose supporting service boundaries.

## Repository Layout

```text
payment-gateway
├── Dockerfile
├── docker-compose.yml
├── docker
│   └── postgres
├── libs
│   └── common
├── services
│   ├── api-gateway
│   ├── auth-service
│   ├── ledger-service
│   ├── notification-service
│   ├── payment-service
│   ├── frontend
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

The frontend entrypoint is [http://localhost:3000](http://localhost:3000). Gateway health is available at [http://localhost:3000/actuator/health](http://localhost:3000/actuator/health).

## Quick Start

### Local with Docker

```bash
cp .env.example .env
docker compose --profile services up --build frontend postgres api-gateway auth-service ledger-service notification-service risk-service settlement-service
```

Then run `payment-service` from the IDE with `SPRING_PROFILES_ACTIVE=dev`.

### Full Docker

```bash
cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services up --build
```

## Docker Architecture

This project uses a clean Docker setup that supports both local debugging and full containerized execution.

The base [docker-compose.yml](/Users/macbookair/Documents/GitHub/payment-gateway/docker-compose.yml) contains PostgreSQL, the API gateway, the Dockerized frontend, and the backend microservices. The backend microservices are marked with the `services` Docker profile, which makes it easy to run one service locally while the rest of the platform stays in Docker.

Service URLs are profile-driven. In local hybrid development, the gateway can call a service running outside Docker through `host.docker.internal`. In full Docker mode, [docker-compose.docker.yml](/Users/macbookair/Documents/GitHub/payment-gateway/docker-compose.docker.yml) overrides those URLs so containers talk to each other by service name.

This approach makes it easy to:

- run one service locally for debugging
- run the remaining services in Docker
- switch to full Docker mode without changing application code
- scale the same pattern across multiple microservices

It also follows stronger engineering practices:

- environment-driven configuration
- no hardcoded service URLs in runtime routing
- clean separation between local and container networking
- flexible, production-friendly service composition
- isolated service databases inside one local PostgreSQL container

## Spring Profiles

Each service now supports four Spring profiles:

- `local`: for running from the IDE on your machine
- `docker`: for running inside Docker
- `stage`: for staging and containerized non-production runs
- `prod`: for production deployments

The default profile is `local`, so an IDE run works without extra service URL setup.

Profile behavior:

- `local` uses Docker-hosted local databases on `localhost:5433`
- `docker` uses Docker-safe defaults such as `postgres` and `host.docker.internal`
- `staging` expects staging environment variables
- `prod` expects production environment variables

This means:

- when you run from the IDE, Spring uses local defaults automatically
- when Docker runs a service, Compose sets `SPRING_PROFILES_ACTIVE=stage` by default
- when staging or production runs a service, you set `SPRING_PROFILES_ACTIVE=stage` or `prod` and provide deployment env vars

If you run `payment-service` from IntelliJ, you usually only need:

```text
SPRING_PROFILES_ACTIVE=dev
```

In Docker, the compose file sets:

```text
SPRING_PROFILES_ACTIVE=stage
```

So the service automatically switches between local, Docker, and deployment-safe settings without code changes.

### Common Commands

Run only PostgreSQL and the gateway, while a service runs from your IDE:

```bash
cp .env.example .env
docker compose --profile services up --build frontend postgres api-gateway auth-service ledger-service notification-service risk-service settlement-service
```

Then run your service locally from the IDE with `SPRING_PROFILES_ACTIVE=dev`.

Run one backend service in Docker:

```bash
docker compose --profile services up --build frontend postgres api-gateway payment-service
```

Run the full platform inside Docker:

```bash
cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services up --build
```

For stage or production, run the application with the matching profile and provide required environment variables such as `DB_URL`, service-specific database URLs, and `JWT_SECRET_KEY`. The `stage` profile is container-friendly, while `prod` stays strict for production deployments.

Open:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Gateway: [http://localhost:8080](http://localhost:8080)
- Payment API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Gateway health through Nginx: [http://localhost:3000/actuator/health](http://localhost:3000/actuator/health)

## Example Flow

### 1. Register

```bash
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName":"Rahul Sharma",
    "email":"rahul@example.com",
    "password":"User@1234"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"rahul@example.com",
    "password":"User@1234"
  }'
```

### 3. Create Order

```bash
curl -X POST http://localhost:3000/api/v1/orders \
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

- Payment roles are bootstrapped automatically in `local` and `docker` profiles, and an admin user is created only when `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` are explicitly provided.
- Shared API wrapper contracts live in `libs/common`.
- `services/api-gateway` includes route configuration to forward `/api/v1/**` to the payment service.
- Service architecture notes live in `docs/ARCHITECTURE.md`.
- GitHub Actions CI is defined in `.github/workflows/ci.yml`.

## What To Showcase On GitHub

- Clear layered package structure under `services/payment-service/src/main/java`.
- Fintech-specific schema and lifecycle modeling.
- Clean docs and architecture narrative.
- Production-friendly defaults without hard-coded local-only hacks.
