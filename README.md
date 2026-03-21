# Fintech Payment Platform

![Java](https://img.shields.io/badge/Java-21-007396?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)
![Security](https://img.shields.io/badge/Security-JWT%20%2B%20RBAC-111827)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture%20%2B%20DDD-0F766E)
![OpenAPI](https://img.shields.io/badge/API-OpenAPI%203-85EA2D)

Production-style fintech backend built with Java 21 and Spring Boot 3.3. The repository is structured like a real payments team codebase: a gateway-ready edge module, a hardened payment domain core, bounded platform services, PostgreSQL persistence, JWT security, pagination and filtering, idempotent payment APIs, auditability, and containerized local runtime.

## Why It Looks Enterprise

- Clean separation into `controller`, `service`, `domain`, `repository`, `config`, `security`, `dto`, and `exception`.
- Domain-driven aggregates for users, roles, orders, payments, transactions, and audit logs.
- JWT authentication with role-based access for `ADMIN` and `USER`.
- Razorpay-style order and payment capture simulation with idempotency protection.
- PostgreSQL-backed domain model with automatic table creation for local development and bootstrap seed data for the payment core.
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
docker compose up --build postgres api-gateway
```

### Local with Maven

```bash
mvn -q -pl services/payment-service -am spring-boot:run
```

## Docker Architecture

This project uses a clean Docker setup that supports both local debugging and full containerized execution.

The base [docker-compose.yml](/Users/macbookair/Documents/GitHub/payment-gateway/docker-compose.yml) contains the core platform services, including PostgreSQL, the API gateway, and all backend microservices. The microservices are marked with the `services` Docker profile, which makes them optional. This means you can start only the services you need instead of running the full stack every time.

Service URLs are controlled through environment variables in `.env`. In local development, the gateway can call services running outside Docker through `host.docker.internal`. This is useful when you want to run one microservice directly from IntelliJ or VS Code for debugging while the rest of the platform stays inside Docker.

When you want everything to run inside Docker, use [docker-compose.docker.yml](/Users/macbookair/Documents/GitHub/payment-gateway/docker-compose.docker.yml). That file overrides the service URLs so containers communicate with each other using Docker service names such as `payment-service`, `auth-service`, and `risk-service`.

This approach makes it easy to:

- run one service locally for debugging
- run the remaining services in Docker
- switch to full Docker mode without changing application code
- scale the same pattern across multiple microservices

It also follows good engineering practices:

- environment-driven configuration
- no hardcoded service URLs
- clean separation between local and container networking
- flexible, production-friendly service composition

## Spring Profiles

Each service now supports four Spring profiles:

- `local`: for running from the IDE on your machine
- `docker`: for running inside Docker
- `staging`: for staging deployments
- `prod`: for production deployments

The default profile is `local`, so an IDE run works without extra service URL setup.

Profile behavior:

- `local` uses local defaults such as `localhost`
- `docker` uses Docker-safe defaults such as `postgres` and `host.docker.internal`
- `staging` expects staging environment variables
- `prod` expects production environment variables

This means:

- when you run from the IDE, Spring uses local defaults automatically
- when Docker runs a service, Compose sets `SPRING_PROFILES_ACTIVE=docker`
- when staging or production runs a service, you set `SPRING_PROFILES_ACTIVE=staging` or `prod` and provide deployment env vars

If you run `payment-service` from IntelliJ, you usually only need:

```text
SPRING_PROFILES_ACTIVE=local
```

In Docker, the compose file sets:

```text
SPRING_PROFILES_ACTIVE=docker
```

So the service automatically switches to Docker-friendly settings such as connecting to `postgres` instead of `localhost`.

### Common Commands

Run only PostgreSQL and the gateway, while a service runs from your IDE:

```bash
cp .env.example .env
docker compose up --build postgres api-gateway
```

Then run your service locally from the IDE with `SPRING_PROFILES_ACTIVE=local`.

Run one backend service in Docker:

```bash
docker compose --profile services up --build postgres payment-service
```

Run the full platform inside Docker:

```bash
cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services up --build
```

For staging or production, run the application with the matching profile and provide the required environment variables such as `DB_URL`, `AUTH_DB_URL`, `LEDGER_DB_URL`, and service URLs.

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

- Payment bootstrap seed data lives in [services/payment-service/src/main/resources/data.sql](/Users/macbookair/Documents/GitHub/payment-gateway/services/payment-service/src/main/resources/data.sql).
- Shared API wrapper contracts live in `libs/common`.
- `services/api-gateway` includes route configuration to forward `/api/v1/**` to the payment service.
- Service architecture notes live in `docs/ARCHITECTURE.md`.
- GitHub Actions CI is defined in `.github/workflows/ci.yml`.

## What To Showcase On GitHub

- Clear layered package structure under `services/payment-service/src/main/java`.
- Fintech-specific schema and lifecycle modeling.
- Clean docs and architecture narrative.
- Production-friendly defaults without hard-coded local-only hacks.
