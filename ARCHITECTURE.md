# PayFlow Architecture

> Enterprise Payment Gateway Architecture

## Monorepo Structure

PayFlow is currently a **monorepo** containing all microservices:

```
payflow/
├── libs/common/              # Shared library (DTOs, exceptions, utils)
├── services/
│   ├── api-gateway/          # Central API gateway (8080)
│   ├── auth-service/         # Authentication & authorization (8081)
│   ├── order-service/        # Orders, merchants, KYC, API keys (8082)
│   ├── payment-service/      # Payment orchestration (8083)
│   ├── notification-service/ # Notifications, webhooks, feature flags (8084)
│   ├── simulator-service/    # Payment simulation & testing (8086)
│   ├── analytics-service/    # Risk, settlements, disputes, reports (8089)
│   └── audit-service/        # MongoDB audit logging (8090)
├── web/
│   ├── payment-page/        # Customer checkout (React 18)
│   └── dashboard/           # Merchant dashboard (Next.js)
├── infra/                    # Infrastructure configs
└── docker-compose.yml        # Infrastructure services
```

## Services

| Service | Port | Tech | Database |
|---------|------|------|----------|
| **api-gateway** | 8080 | Spring Cloud Gateway | Redis |
| **auth-service** | 8081 | Spring Boot 3 | PostgreSQL, Redis |
| **order-service** | 8082 | Spring Boot 3 | PostgreSQL |
| **payment-service** | 8083 | Spring Boot 3 | PostgreSQL, Redis |
| **notification-service** | 8084 | Spring Boot 3 | PostgreSQL |
| **simulator-service** | 8086 | Spring Boot 3 | PostgreSQL |
| **analytics-service** | 8089 | Spring Boot 3 | PostgreSQL |
| **audit-service** | 8090 | Spring Boot 3 | MongoDB |

## Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database |
| MongoDB | 27017 | Audit logs |
| Redis | 6379 | Cache, rate limiting |
| Kafka | 9092 | Event streaming |
| Zipkin | 9411 | Distributed tracing |

## Communication Patterns

### Synchronous (REST/Feign)
- API Gateway → All services
- Auth → Order → Payment

### Asynchronous (Kafka)
- Payment → Notification
- Payment → Analytics
- Payment → Audit

## Security

- JWT authentication with refresh tokens
- Rate limiting (Redis-based)
- Circuit breaking (Resilience4j)
- API keys for merchant integrations
- PCI DSS compliance (tokenization)

## Planned Services

- **graphql-gateway** (8087) - GraphQL API with federation
- **search-service** (8088) - Elasticsearch-powered search
