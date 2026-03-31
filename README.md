# Fintech Payment Gateway

Production-Grade Microservices Architecture

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

A production-grade fintech payment platform built with Java 21, Spring Boot 3.3, and event-driven microservices architecture. Designed for high-scale payment processing with robust security, observability, and reliability patterns.

---

## Architecture Overview

```
+---------------------------------------------------------------------------------------+
|                                    CLIENTS                                           |
|                          (Browser / Mobile App)                                      |
+--------------------------------------+----------------------------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------------+
|                              API GATEWAY (8080)                                       |
|  +------------------+  +------------------+  +------------------+                   |
|  |  Rate Limiting   |  |   JWT Validation |  |  Request Routing |                   |
|  |    (Redis)       |  |                  |  |                  |                   |
|  +------------------+  +------------------+  +------------------+                   |
+--------------------------------------+----------------------------------------------+
                                       |
         +-----------------------------+-----------------------------+
         |                             |                             |
         v                             v                             v
+---------------+          +-------------------+          +-------------------+
| Auth Service  |          |  Order Service    |          |  Payment Service  |
|    (8081)     |          |     (8082)        |          |      (8083)       |
+---------------+          +-------------------+          +-------------------+
        |                             |                             |
        |                             |                             |
        v                             v                             v
+---------------+          +-------------------+          +-------------------+
|  PostgreSQL   |          |    PostgreSQL     |          |    PostgreSQL     |
|   (authdb)    |          |    (orderdb)       |          |   (paymentdb)    |
+---------------+          +-------------------+          +-------------------+
         |                             |                             |
         |                             |                             |
         +-----------------------------+-----------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------------+
|                                     KAFKA                                             |
|                              (Event Bus - 9092)                                       |
|  +------------------+  +------------------+  +------------------+                   |
|  | payment.events  |  |  order.events    |  | notification.events                 |
|  +------------------+  +------------------+  +------------------+                   |
+--------------------------------------+-----------------------------------------------+
         |                             |                             |
         v                             v                             v
+-------------------+    +-------------------+    +-------------------+
| Notification Svc  |    |   Webhook Svc     |    |  Simulator Svc    |
|      (8084)       |    |      (8085)       |    |      (8086)       |
+-------------------+    +-------------------+    +-------------------+
         |                             |
         v                             v
+-------------------+    +-------------------+
|  PostgreSQL       |    |    PostgreSQL     |
| (notificationdb)  |    |   (webhookdb)     |
+-------------------+    +-------------------+

+---------------------------------------------------------------------------------------+
|                                    REDIS                                              |
|                              (Rate Limiting - 6379)                                  |
+---------------------------------------------------------------------------------------+
```

---

## Data Flow

### 1. User Authentication Flow

```
┌──────────┐     ┌───────────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │────>│ API Gateway  │────>│ Auth Svc    │────>│  PostgreSQL  │
│          │     │   (8080)     │     │  (8081)     │     │  (authdb)    │
└──────────┘     └───────────────┘     └─────────────┘     └──────────────┘
      │                                        │
      │  POST /api/v1/auth/register            │
      │  POST /api/v1/auth/login               │
      │                                        │
      │<───────────────────────────────────────┘
      │        JWT Access + Refresh Token
```

1. User sends credentials to `/api/v1/auth/register` or `/api/v1/auth/login`
2. API Gateway validates request, applies rate limiting
3. Auth Service validates credentials against PostgreSQL
4. Auth Service issues JWT (access token + refresh token)
5. Subsequent requests include JWT in Authorization header

### 2. Order Creation Flow

```
┌──────────┐     ┌───────────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │────>│ API Gateway  │────>│ Order Svc   │────>│  PostgreSQL  │
│          │     │   (8080)     │     │  (8082)     │     │  (orderdb)   │
└──────────┘     └───────────────┘     └─────────────┘     └──────────────┘
      │                                        │
      │  POST /api/v1/orders                   │
      │  Authorization: Bearer <JWT>          │
      │                                        │
      │<───────────────────────────────────────┘
      │        Order ID + Amount
```

1. Authenticated user creates order via `/api/v1/orders`
2. Order Service validates JWT and creates order in PostgreSQL
3. Order Service emits Kafka event for downstream services
4. Payment Service consumes event and awaits payment

### 3. Payment Processing Flow

```
┌──────────┐     ┌───────────────┐     ┌─────────────┐     ┌──────────────┐
│  Client  │────>│ API Gateway  │────>│ Payment Svc  │────>│  PostgreSQL  │
│          │     │   (8080)     │     │  (8083)     │     │  (paymentdb) │
└──────────┘     └───────────────┘     └─────────────┘     └──────────────┘
      │                                        |                     │
      │  POST /api/v1/payments                 |                     │
      │  Idempotency-Key: <key>                v                     v
      │                                        |              +--------------+
      │                                        |              │  Simulator   │
      │                                        |              │    (8086)    │
      │                                        |              +--------------+
      │                                              |
      │<─────────────────────────────────────────────┘
      │            Payment Entity + Provider Intent
      │
      v
┌─────────────────────────────────────────────────────────────┐
│                    PAYMENT LIFECYCLE                         │
│  CREATED ──> AUTHORIZED ──> CAPTURED ──> COMPLETED           │
│                          │                                   │
                          v                                   │
                    +-----------+                               │
                    │  Refunded │ ──> PARTIAL/FULL             │
                    +-----------+                               │
└─────────────────────────────────────────────────────────────┘
```

1. Client initiates payment with `Idempotency-Key` header
2. Payment Service generates provider payment intent
3. Simulator processes payment (async via Kafka)
4. Payment status updates through lifecycle
5. Webhook events trigger final state updates
6. Notifications sent via Notification Service

### 4. Webhook Callback Flow

```
┌──────────────┐     ┌───────────────┐     ┌─────────────┐     ┌──────────────┐
│  External    │────>│  Webhook Svc  │────>│ Payment Svc │────>│  PostgreSQL  │
│  Provider    │     │    (8085)     │     │  (8083)     │     │  (paymentdb) │
└──────────────┘     └───────────────┘     └─────────────┘     └──────────────┘
      │                     |                                        |
      │  HMAC Signature     |         +-----------+                 |
      │                     +────────>│  Kafka    │                 |
      │                     │         │  (events) │                 |
      │                     │         +-----------+                 |
      │                     v                                        |
      │              ┌──────────────┐                               │
      │              │ Dedup Check  │ (event_id)                    │
      │              └──────────────┘                               │
```

1. External provider sends webhook with HMAC-SHA256 signature
2. Webhook Service validates signature
3. Webhook Service checks for duplicate events (dedup by event_id)
4. Events published to Kafka
5. Payment Service consumes and processes
6. Idempotent processing ensures data consistency

### 5. Notification Flow

```
┌──────────────┐     ┌───────────────┐     ┌─────────────┐
│    Kafka     │────>│ Notification  │────>│  PostgreSQL │
│   (events)   │     │    Svc (8084) │     │(notificationdb)
└──────────────┘     └───────────────┘     └─────────────┘
                            │
                            v
                     ┌──────────────┐
                     │   Email/SMS  │
                     │   Provider   │
                     └──────────────┘
```

1. Kafka events trigger notification processing
2. Notification Service consumes events idempotently
3. Persists notification record in PostgreSQL
4. Sends notification via external provider
5. Retry logic with exponential backoff
6. Dead-letter handling for failed notifications

---

## Service Descriptions

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Edge routing, JWT validation, rate limiting, correlation ID management |
| **Auth Service** | 8081 | User registration, login, JWT issuance, token refresh |
| **Order Service** | 8082 | Order creation, management, integration with payment service |
| **Payment Service** | 8083 | Payment processing, idempotency, refunds, ledger entries |
| **Notification Service** | 8084 | Email/SMS notifications, event-driven notification handling |
| **Webhook Service** | 8085 | Webhook receiver, HMAC validation, event deduplication |
| **Simulator Service** | 8086 | Payment provider simulator for testing |

---

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | User login |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/logout` | User logout |

### Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create new order |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders` | List orders |

### Payment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Create payment |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments` | List payments |
| POST | `/api/v1/payments/{id}/refunds` | Process refund |
| GET | `/api/v1/payments/{id}/refunds` | List refunds |

### Webhook Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/webhooks` | Receive webhook |

---

## Quick Start

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Engine
- Docker Compose

### Environment Variables

Create a `.env` file in the project root:

```bash
JWT_SECRET_B64=dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNo
GATEWAY_INTERNAL_SECRET=dev-gateway-internal-secret
```

### Docker Compose Setup

```bash
# Start all services
docker compose --profile services up --build

# Start infrastructure only (for local development)
docker compose --profile infra up -d
```

### Running Locally

**Hybrid Mode (Recommended):**

```bash
# Start infrastructure + gateway
docker compose --profile services up -d postgres kafka redis api-gateway

# Run payment-service locally
./mvnw -pl services/payment-service -am spring-boot:run
```

**Full Docker:**

```bash
docker compose --profile services up --build
```

### Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3001 |
| Prometheus | http://localhost:9090 |
| Zipkin | http://localhost:9411 |

---

## Development

### Building Services

```bash
# Build all services
mvn clean install

# Build specific service
mvn -pl services/payment-service clean package
```

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests with Testcontainers
mvn -pl services/payment-service -Ptestcontainers -Dtest=PaymentFlowContainersIntegrationTest test

# All tests with verification
mvn verify
```

### Code Structure

```
payment-gateway/
├── libs/common/                    # Shared libraries
│   └── src/main/java/dev/payment/common/
│       ├── api/                    # Common API models
│       ├── dto/                    # Data transfer objects
│       └── events/                 # Kafka event definitions
├── services/
│   ├── api-gateway/                # Edge routing & rate limiting
│   ├── auth-service/               # Authentication & JWT
│   ├── order-service/              # Order management
│   ├── payment-service/            # Payment processing
│   ├── notification-service/       # Notifications
│   ├── webhook-service/            # Webhook handling
│   └── simulator-service/          # Payment simulator
├── docker/                         # Docker configurations
├── config/                         # Service configurations
├── scripts/                        # Development scripts
└── ops/                            # Operational configs (K8s, Grafana)
```

---

## Configuration

### Environment Variables

#### Common Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | Service-specific |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `docker` |
| `JWT_SECRET_B64` | Base64-encoded JWT secret | Required |
| `GATEWAY_INTERNAL_SECRET` | Internal gateway secret | Required |

#### Database Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `postgres` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | Service-specific |
| `DB_USERNAME` | Database user | Service-specific |
| `DB_PASSWORD` | Database password | Service-specific |

#### Kafka Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `kafka:29092` |

#### Redis Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis host | `redis` |
| `REDIS_PORT` | Redis port | `6379` |

#### Service URLs

| Variable | Description | Default |
|----------|-------------|---------|
| `AUTH_SERVICE_URL` | Auth service URL | `http://auth-service:8081` |
| `ORDER_SERVICE_URL` | Order service URL | `http://order-service:8082` |
| `PAYMENT_SERVICE_URL` | Payment service URL | `http://payment-service:8083` |
| `NOTIFICATION_SERVICE_URL` | Notification service URL | `http://notification-service:8084` |
| `SIMULATOR_SERVICE_URL` | Simulator service URL | `http://simulator-service:8086` |

---

## Technology Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.3 | Framework |
| Spring Cloud | 2023.0 | Cloud Native |
| Spring Security | 6.x | Security |
| Spring Data JPA | 3.3 | Data Access |
| Spring Kafka | 3.2 | Event Streaming |

### Data & Messaging

| Technology | Version | Purpose |
|------------|---------|---------|
| PostgreSQL | 16 | Primary Database |
| Kafka | 3.8 | Event Bus |
| Redis | 7.4 | Caching & Rate Limiting |

### Observability

| Technology | Version | Purpose |
|------------|---------|---------|
| Prometheus | - | Metrics |
| Grafana | - | Dashboards |
| Zipkin | - | Distributed Tracing |
| Micrometer | - | Metrics API |

### Build & Deploy

| Technology | Version | Purpose |
|------------|---------|---------|
| Maven | 3.9+ | Build Tool |
| Docker | - | Containerization |
| Docker Compose | - | Orchestration |

---

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Getting Started

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Workflow

```bash
# Run tests before committing
mvn test

# Verify build
mvn verify

# Check code style (if configured)
mvn checkstyle:check
```

### Pull Request Requirements

- [ ] All tests passing
- [ ] Code follows project conventions
- [ ] Documentation updated for externally visible changes
- [ ] Clear commit messages

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Support

For issues and questions, please open an issue on GitHub.

---

**Built with passion for fintech innovation**
