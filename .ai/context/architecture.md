# Architecture Overview

> PayFlow system architecture for AI context

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│  ┌──────────────────┐              ┌──────────────────────────┐ │
│  │  Checkout UI     │              │  GraphQL Playground      │ │
│  │ (web/frontend)   │              │  (localhost:8087)        │ │
│  └────────┬─────────┘              └───────────┬──────────────┘ │
└───────────┼─────────────────────────────────────┼────────────────┘
            │                                     │
            ▼                                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              API Gateway (8080) - Spring Cloud Gateway          │
│  ┌────────────┬────────────┬────────────┬──────────────────┐   │
│  │ JWT Auth   │ Rate Limit │  CORS      │ Circuit Breaker  │   │
│  │ Validation │ (Redis)    │  Security  │  (Resilience4j)   │   │
│  └────────────┴────────────┴────────────┴──────────────────┘   │
└─────────────────────────────┬───────────────────────────────────┘
                              │
    ┌───────────┬─────────────┼─────────────┬───────────┐
    ▼           ▼             ▼             ▼           ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐
│  Auth  │ │ Order  │ │ Payment  │ │ Notif    │ │Analytics│
│ 8081   │ │ 8082   │ │   8083   │ │  8084    │ │  8089   │
└────────┘ └────────┘ └──────────┘ └──────────┘ └─────────┘
                              │
    ┌───────────┬─────────────┼─────────────┬───────────┐
    ▼           ▼             ▼             ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│Simulator │ │  Search  │ │  Audit   │ │ GraphQL  │ │ Notif    │
│  (8086)  │ │  (8088)  │ │  (8089)  │ │ (8087)   │ │ Consumer │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Event-Driven Messaging                       │
│              Apache Kafka (payment.*, order.*)                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │
    ┌───────────┬─────────────┼─────────────┬───────────┐
    ▼           ▼             ▼             ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│PostgreSQL│ │ MongoDB  │ │  Redis   │ │Elasticse.│ │   Kafka  │
│  (5432)  │ │ (27017)  │ │ (6379)   │ │ (9200)   │ │ (9092)   │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

---

## 2. Technology Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.3, Spring Cloud |
| **API Gateway** | Spring Cloud Gateway (WebFlux) |
| **GraphQL** | Spring GraphQL |
| **Database** | PostgreSQL 16, MongoDB 7, Redis 7 |
| **Search** | Elasticsearch 8 |
| **Messaging** | Apache Kafka 3.7 |
| **Security** | Spring Security, JWT (HS512), BCrypt |
| **Resilience** | Resilience4j |
| **Observability** | OpenTelemetry, Prometheus, Grafana |
| **Frontend** | React 18, Vite 5, Tailwind CSS |
| **Container** | Docker, Docker Compose |
| **Build** | Maven 3.9 |

---

## 3. Infrastructure

### Docker Services

| Service | Port | Purpose |
|---------|------|---------|
| api-gateway | 8080 | REST API entry point |
| auth-service | 8081 | JWT auth, OAuth2, RBAC |
| order-service | 8082 | Orders, merchants, KYC |
| payment-service | 8083 | Payment orchestration |
| notification-service | 8084 | Email, SMS, push, webhooks |
| simulator-service | 8086 | Payment simulation, testing |
| graphql-gateway | 8087 | GraphQL API |
| search-service | 8088 | Elasticsearch search |
| analytics-service | 8089 | Reports, metrics, risk |
| audit-service | 8089 | MongoDB audit logging |

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database |
| MongoDB | 27017 | Audit logs, events |
| Redis | 6379 | Cache, rate limiting |
| Elasticsearch | 9200 | Full-text search |
| Kafka | 9092 | Event streaming |
| Zookeeper | 2181 | Kafka coordination |
| Grafana | 3000 | Dashboards |
| Prometheus | 9090 | Metrics |
| Jaeger | 16686 | Distributed tracing |

---

## 4. Security Architecture

### Authentication Flow

```
User → API Gateway → Auth Service → JWT Token
                                    ↓
                              Validate Token
                                    ↓
                         Authorize Request → Service
```

### Key Security Features

- **JWT**: HS512 signed tokens with refresh flow
- **RBAC**: Admin, Merchant, User roles
- **Rate Limiting**: Token bucket (1000 req/min via Redis)
- **API Keys**: HMAC-signed for merchant integrations
- **PCI DSS**: Card tokenization (never store card data)

---

## 5. Event Architecture

### Kafka Topics

| Topic | Purpose |
|-------|---------|
| payment.created | New payment created |
| payment.completed | Payment succeeded |
| payment.failed | Payment failed |
| payment.refunded | Payment refunded |
| order.created | New order created |
| order.updated | Order status changed |
| webhook.updates | Webhook delivery events |
| audit.events | Audit log events |

---

## 6. Project Structure

```
payflow/
├── libs/common/              # Shared DTOs, exceptions, utils
├── services/
│   ├── api-gateway/          # Spring Cloud Gateway
│   ├── auth-service/         # Authentication & authorization
│   ├── order-service/        # Order management
│   ├── payment-service/      # Payment orchestration
│   ├── notification-service/ # Notifications
│   ├── simulator-service/    # Payment simulation
│   ├── graphql-gateway/      # GraphQL API
│   ├── search-service/       # Elasticsearch
│   ├── analytics-service/    # Analytics & reporting
│   └── audit-service/        # Audit logging
├── web/frontend/             # React checkout UI
├── infra/                    # Infrastructure configs
└── scripts/                  # Dev scripts
```