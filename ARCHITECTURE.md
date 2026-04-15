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
│   ├── order-service/        # Orders, merchants, API keys (8082)
│   ├── payment-service/      # Payment orchestration (8083)
│   ├── notification-service/ # Notifications, webhooks (8084)
│   └── simulator-service/    # Payment simulation (8086)
├── web/
│   └── payment-page/        # Customer checkout (React 18)
└── docker-compose.yml       # Infrastructure services
```

## Services

| Service | Port | Tech | Database |
|---------|------|------|----------|
| **api-gateway** | 8080 | Spring Cloud Gateway | Redis |
| **auth-service** | 8081 | Spring Boot 3 | PostgreSQL |
| **order-service** | 8082 | Spring Boot 3 | PostgreSQL |
| **payment-service** | 8083 | Spring Boot 3 | PostgreSQL |
| **notification-service** | 8084 | Spring Boot 3 | PostgreSQL |
| **simulator-service** | 8086 | Spring Boot 3 | PostgreSQL |

## Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache, rate limiting |
| Kafka | 9092 | Event streaming |

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
