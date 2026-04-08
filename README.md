# PayFlow — Enterprise Payment Gateway

> A production-grade, cloud-native payment processing platform built with Spring Boot 3, Kafka, PostgreSQL, MongoDB, and React. Designed for high-throughput, low-latency payment orchestration with real-time analytics, multi-currency support, and enterprise-grade security.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8-005571?logo=elasticsearch&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

---

## Architecture

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                              Client Layer                                      │
│  ┌──────────────────┐              ┌──────────────────────────────────────┐  │
│  │  Checkout UI      │              │  GraphQL Playground (GraphiQL)        │  │
│  │  (web/frontend)   │              │  localhost:8087/graphiql              │  │
│  └────────┬─────────┘              └──────────────────┬───────────────────┘  │
└───────────┼─────────────────────────────────────────────┼─────────────────────┘
            │                                              │
            ▼                                              ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                     API Gateway (Spring Cloud Gateway)                          │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ JWT Auth    │  │ Rate Limiting│  │ CORS Policy  │  │ Circuit Breaker │  │
│  │ Validation  │  │ (Redis)      │  │ Security     │  │ (Resilience4j)  │  │
│  └─────────────┘  └──────────────┘  └──────────────┘  └─────────────────┘  │
└─────────────────────────────┬────────────────────────────────────────────────┘
                               │
                     ┌───────────────────┼───────────────────┬───────────────────┐
                     ▼                   ▼                   ▼                   ▼
             ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌──────────────────┐
              │  Auth Service    │ │Order Service │ │  Payment Service  │ │ GraphQL Gateway  │
              │  (8081)          │ │  (8082)      │ │  (8083)           │ │  (8087)          │
             │                  │ │              │ │                  │ │                  │
             │  • JWT Auth     │ │  • Orders   │ │  • Payments     │ │  • GraphQL API   │
             │  • OAuth2       │ │  • Merchants│ │  • Refunds      │ │  • Federation    │
             │  • RBAC         │ │  • API Keys │ │  • Webhooks     │ │  • DataLoader    │
             │  • Sessions     │ │  • KYC      │ │  • Idempotency  │ │                  │
             └──────────────────┘ └──────────────┘ └──────────────────┘ └──────────────────┘
                              │
                    ┌───────────────────┼───────────────────┬───────────────────┐
                    ▼                   ▼                   ▼                   ▼
            ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌──────────────────┐
            │ Notification     │ │ Analytics    │ │  Simulator       │ │  Search Service  │
            │ Service (8084)   │ │ Service      │ │  Service (8086)  │ │  (8088)          │
            │                  │ │  (8089)      │ │                  │ │                  │
            │  • Email/SMS    │ │  • Reports  │ │  • Load Testing │ │  • Elasticsearch │
            │  • Push         │ │  • Metrics  │ │  • Demo Mode    │ │  • Full-text    │
            │  • Webhooks     │ │  • Dashboards│ │  • Mock Providers│ │  • Aggregations │
            │  • Feature Flags│ │  • Alerts   │ │                  │ │                  │
            └──────────────────┘ └──────────────┘ └──────────────┘ └──────────────────┘
                              │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
            ┌───────────────────────────────────────────────────────────────────────────────┐
            │                        Event-Driven Messaging (Kafka)                          │
            │  payment.created │ payment.completed │ order.events │ webhook.updates │ audit  │
            └─────────────────────────────┬────────────────────────────────────────────────┘
                              │
                    ┌───────────────────┼───────────────────┬───────────────────┐
                    ▼                   ▼                   ▼                   ▼
            ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌──────────────────┐
            │ Notification     │ │ Analytics    │ │  Simulator       │ │  Search Service  │
            │ Service (8084)   │ │ Service      │ │  Service (8086)  │ │  (8088)          │
            │                  │ │  (8089)      │ │                  │ │                  │
            │  • Email/SMS    │ │  • Reports  │ │  • Load Testing │ │  • Elasticsearch │
            │  • Push         │ │  • Metrics  │ │  • Dashboards│ │  • Mock Providers│ │  • Aggregations │
            │  • Feature Flags│ │  • Alerts   │ │                  │ │                  │
            └──────────────────┘ └──────────────┘ └──────────────┘ └──────────────────┘
                              │
                              ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                          Audit Service (8089)                                  │
│                                                                               │
│  • MongoDB-based audit logging                                                │
│  • Event sourcing support                                                     │
│  • Compliance & regulatory requirements                                        │
│  • User activity tracking                                                     │
└───────────────────────────────────────────────────────────────────────────────┘

## Infrastructure

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                            Infrastructure                                     │
│                                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ PostgreSQL   │  │   MongoDB    │  │    Redis     │  │  Elasticsearch   │  │
│  │ (Primary DB) │  │ (Audit Logs) │  │  (Cache)     │  │  (Search)        │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Kafka      │  │  Prometheus  │  │   Grafana    │  │    Jaeger        │  │
│  │ (Events)     │  │  (Metrics)   │  │  (Dashboards)│  │  (Tracing)       │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Services

| Service | Port | Description | Tech |
|---------|------|-------------|------|
| **api-gateway** | 8080 | Central routing, auth, rate limiting | Spring Cloud Gateway |
| **auth-service** | 8081 | JWT auth, OAuth2, RBAC, sessions | Spring Security |
| **order-service** | 8082 | Order management, merchants, KYC, API keys | Spring Data JPA |
| **payment-service** | 8083 | Payment orchestration, multi-provider integration | Spring Boot |
| **notification-service** | 8084 | Email, SMS, push, webhooks, feature flags | Kafka, Redis |
| **simulator-service** | 8086 | Payment simulation, load testing, demo mode | Spring Boot |
| **graphql-gateway** | 8087 | GraphQL API with schema federation | Spring GraphQL |
| **search-service** | 8088 | Full-text search, aggregations | Elasticsearch |
| **analytics-service** | 8089 | Risk scoring, settlements, disputes, reports | Kafka, JPA |
| **audit-service** | 8089 | Audit logging, compliance | MongoDB |

## Features

### Payment Processing
- **Multi-Provider**: Stripe, Razorpay, PayPal integration
- **Multi-Currency**: 150+ currencies with real-time conversion
- **Smart Routing**: Automatic provider selection based on cost/success rate
- **Idempotency**: Guaranteed exactly-once processing with idempotency keys
- **Retry Logic**: Exponential backoff with circuit breaker (Resilience4j)

### GraphQL API
- **Schema Federation**: GraphQL gateway aggregates data from multiple services
- **Real-time Subscriptions**: WebSocket-based live updates
- **DataLoader**: N+1 query optimization
- **GraphiQL UI**: Interactive API playground at `/graphiql`

### Search & Analytics
- **Full-text Search**: Elasticsearch-powered payment and order search
- **Aggregations**: Revenue analytics, payment trends, merchant reports
- **Real-time Dashboards**: Live metrics via WebSocket

### Security
- **JWT Authentication**: HS512 signed tokens with refresh flow
- **RBAC**: Admin, Merchant, User roles with method-level security
- **Rate Limiting**: Token bucket algorithm via Redis (1000 req/min per user)
- **API Keys**: HMAC-signed keys for merchant integrations
- **PCI DSS**: Card data never touches our servers (tokenization)
- **Secrets Management**: Zero hardcoded secrets via environment variables

### Polyglot Persistence
- **PostgreSQL**: Primary datastore for payments, orders, users
- **MongoDB**: Audit logs, event sourcing, flexible schemas
- **Redis**: Caching, rate limiting, session management
- **Elasticsearch**: Full-text search, analytics aggregations

### Real-Time
- **Event Streaming**: Kafka for all payment state changes
- **WebSocket**: Live payment status updates
- **Webhooks**: Configurable event delivery to merchant endpoints
- **Feature Flags**: Runtime toggling without restarts

### Observability
- **Metrics**: Prometheus + Micrometer (100+ custom metrics)
- **Tracing**: OpenTelemetry + Jaeger distributed tracing
- **Dashboards**: Pre-built Grafana dashboards
- **Health Checks**: Kubernetes-ready liveness/readiness probes

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Node.js 22+ (for web apps)
- Maven 3.9+

### Run with Docker (Recommended)
```bash
# Start all infrastructure + services
docker compose up -d

# Check service health
docker compose ps

# View logs
docker compose logs -f
```

### Run Monitoring Stack
```bash
docker compose -f docker-compose.monitoring.yml up -d

# Access dashboards
# - Grafana: http://localhost:3000 (admin/admin)
# - Prometheus: http://localhost:9090
# - Jaeger: http://localhost:16686
```

### Run in Dev Mode (Hot Reload)
```bash
# Start infrastructure only
docker compose --profile infra up -d

# Run services locally with Maven
./scripts/dev.sh start

# Start web apps
cd web/frontend && npm run dev
```

### Build
```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
docker build -t payment-gateway/api-gateway:latest -f services/api-gateway/Dockerfile .
```

## API Endpoints

### REST API (via API Gateway)
```bash
# Health check
curl http://localhost:8080/actuator/health

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflow.dev","password":"Demo@1234","firstName":"Demo","lastName":"User"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@payflow.com","password":"Test@1234"}'

# Create Order (requires auth token)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"currency":"USD","customerEmail":"test@example.com","description":"Test Order"}'

# Create Payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<order-id>","provider":"STRIPE","paymentMethod":"CARD"}'
```

### GraphQL API
```bash
# Access GraphiQL playground
open http://localhost:8087/graphiql

# Example queries
query {
  payments(page: 0, pageSize: 10) {
    payments {
      id
      amount
      status
    }
    totalCount
  }
}

query {
  analyticsSummary(startDate: "2026-01-01", endDate: "2026-04-01") {
    totalTransactions
    totalVolume
    successRate
  }
}
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.3, Spring Cloud 2023 |
| **Database** | PostgreSQL 16, MongoDB 7, Redis 7 |
| **Search** | Elasticsearch 8 |
| **Messaging** | Apache Kafka 3.7 |
| **Gateway** | Spring Cloud Gateway (WebFlux), Spring GraphQL |
| **Security** | Spring Security, JWT (JJWT), BCrypt |
| **Resilience** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| **Observability** | OpenTelemetry, Prometheus, Grafana, Jaeger |
| **Container** | Docker, Docker Compose, Kubernetes-ready |
| **Build** | Maven 3.9 |

## Project Structure

```
payment-gateway/
├── libs/common/              # Shared library (DTOs, exceptions, utils)
├── services/
│   ├── api-gateway/          # Central API gateway (8080)
│   ├── auth-service/         # Authentication & authorization (8081)
│   ├── order-service/        # Orders, merchants, KYC, API keys (8082)
│   ├── payment-service/      # Payment orchestration (8083)
│   ├── notification-service/ # Notifications, webhooks, feature flags (8084)
│   ├── simulator-service/    # Payment simulation & testing (8086)
│   ├── graphql-gateway/      # GraphQL API with federation (8087)
│   ├── search-service/       # Elasticsearch search (8088)
│   ├── analytics-service/    # Risk, settlements, disputes, reports (8089)
│   └── audit-service/        # MongoDB audit logging (8089)
├── web/
│   └── frontend/             # Customer checkout (React)
├── database/                 # Database initialization scripts
├── infra/                    # Infrastructure configs (Kafka, Postgres, Redis, Vault)
├── k8s/
│   └── base/                 # Kubernetes manifests
├── tests/                    # Integration and E2E tests
├── docs/                     # Architecture and compliance documentation
└── scripts/                  # Development scripts
```

## Kubernetes Deployment

```bash
# Apply all services
kubectl apply -f k8s/base/

# Check deployment status
kubectl get pods -l 'app in (api-gateway,payment-service,auth-service)'

# View logs
kubectl logs -l app=api-gateway -f
```

## CI/CD

GitHub Actions workflow includes:
- **Build**: Compiles all services and builds Docker images
- **Test**: Unit, integration, and E2E tests
- **Security**: Trivy vulnerability scanning and secret detection
- **Deploy**: Pushes images to GitHub Container Registry

## Documentation

- [docs/SERVICES.md](docs/SERVICES.md) - Complete service reference
- [docs/API.md](docs/API.md) - API endpoints and examples
- [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) - Infrastructure details
- [.ai/context/architecture.md](.ai/context/architecture.md) - Architecture overview
- [.ai/context/services.md](.ai/context/services.md) - Service context

## License

MIT License — see [LICENSE](LICENSE)
