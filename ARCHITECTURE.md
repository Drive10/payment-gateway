# Payment Gateway - Architecture Documentation

## Overview

The Payment Gateway is a microservices-based payment processing system built with Spring Boot and React.

## Technology Stack

### Data Stores
| Component | Purpose | Docker Port |
|-----------|---------|-------------|
| PostgreSQL | Transactional data (payments, orders, users) | 5433 |
| MongoDB | Logs, audit trails, event storage | 27017 |
| Redis | Session cache, rate limiting, distributed cache | 6379 |

### Messaging & Streaming
| Component | Purpose | Docker Port |
|-----------|---------|-------------|
| Apache Kafka | Event streaming, async communication | 9092 |
| Zookeeper | Kafka cluster management | 2181 |
| Kafka UI | Kafka topic management UI | 8090 |

### Search & Analytics
| Component | Purpose | Docker Port |
|-----------|---------|-------------|
| Elasticsearch | Full-text search, log aggregation | 9200 |
| Kibana | Elasticsearch visualization | 5601 |

### Observability
| Component | Purpose | Docker Port |
|-----------|---------|-------------|
| Prometheus | Metrics collection | 9090 |
| Grafana | Metrics visualization & dashboards | 3001 |
| Jaeger | Distributed tracing | 16686 |

### Service Registry
| Component | Purpose | Docker Port |
|-----------|---------|-------------|
| Consul | Service discovery | 8500 |

## Service Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Clients                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                        │
│  - Authentication                                          │
│  - Rate Limiting                                          │
│  - Request Routing                                        │
└───────┬───────┬───────┬───────┬───────┬───────┬─────────┘
        │       │       │       │       │       │
        ▼       ▼       ▼       ▼       ▼       ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  Auth   │ │  Order  │ │ Payment │ │Notifica-│ │Analytics│
   │(8081)   │ │(8082)   │ │(8083)   │ │tion(8084)│ │(8085)   │
   └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘
        │           │           │           │           │
        └───────────┴───────────┴───────────┴───────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │    PostgreSQL    │
                     │  (Transactions)  │
                     └────────────────┘
```

## Running the Infrastructure

### Start All Infrastructure
```bash
# Production infrastructure (all services)
docker compose -f docker-compose.prod.yml up -d

# Development infrastructure (basic)
docker compose -f docker-compose.yml up -d

# Monitoring stack only
docker compose -f docker-compose.monitoring.yml up -d
```

### Access URLs
- **API Gateway**: http://localhost:8080
- **PostgreSQL**: localhost:5433
- **MongoDB**: localhost:27017
- **Redis**: localhost:6379
- **Kafka**: localhost:9092
- **Kafka UI**: http://localhost:8090
- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Jaeger**: http://localhost:16686
- **Consul**: http://localhost:8500

## Development

### Prerequisites
- Java 21
- Node.js 20
- Docker Desktop
- Maven 3.9+

### Start Development Environment
```bash
# Start infrastructure
docker compose -f docker-compose.yml up -d postgres redis kafka

# Start all services locally with hot reload
./scripts/dev.sh up
```

### Service Endpoints
| Service | Local Port | Health Endpoint |
|---------|------------|-----------------|
| API Gateway | 8080 | /actuator/health |
| Auth Service | 8081 | /actuator/health |
| Order Service | 8082 | /actuator/health |
| Payment Service | 8083 | /actuator/health |
| Notification Service | 8084 | /actuator/health |
| Analytics Service | 8085 | /actuator/health |
| Simulator Service | 8086 | /actuator/health |

## CI/CD Pipeline

The project uses GitHub Actions for CI/CD with the following workflow:

1. **Build & Test**
   - Runs Maven build for all backend services
   - Runs npm build and lint for frontend
   - Executes unit tests

2. **Security Scan**
   - Trivy vulnerability scanning
   - CodeQL analysis

3. **Docker Build & Push**
   - Builds Docker images for each service
   - Pushes to GitHub Container Registry

4. **Deploy**
   - Deploys to staging on `develop` branch
   - Deploys to production on `main` branch

## Monitoring & Observability

### Prometheus Metrics
Each service exposes metrics at `/actuator/prometheus`:
- HTTP request counts and latencies
- JVM metrics (memory, GC, threads)
- Database connection pool metrics
- Custom business metrics

### Grafana Dashboards
Pre-configured dashboards for:
- Service overview
- Request rates and latencies
- Error rates
- JVM performance
- Database connections

### Distributed Tracing
Jaeger provides:
- Request traces across services
- Performance bottlenecks identification
- Error tracking

## Security

- All secrets managed via Vault in production
- JWT-based authentication
- Rate limiting on API Gateway
- CORS configuration
- Security headers (CSP, X-Frame-Options, etc.)

## Environment Variables

See `.env.example` for all configurable environment variables.

### Required for Production
- `POSTGRES_PASSWORD` - PostgreSQL password
- `REDIS_PASSWORD` - Redis password
- `JWT_SECRET` - Base64-encoded JWT signing key
- `VAULT_TOKEN` - Vault access token

## License

Proprietary - All rights reserved
