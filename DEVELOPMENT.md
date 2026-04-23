# PayFlow Development Guide

## Quick Start

### Full Development Environment
```bash
make dev  # Starts all infrastructure, services, and frontend
```

### Selective Service Development
When working on a specific service, you can start just what you need:

```bash
# Start infrastructure only
make infra-up

# Start payment service with local profile
make dev-payment

# Start frontend only
make dev-frontend

# Start payment service + frontend (lightweight)
make dev-lite
```

## Available Development Targets

| Target | Description |
|--------|-------------|
| `make dev-payment` | Payment service with local dev profile |
| `make dev-frontend` | Frontend development server |
| `make dev-gateway` | API Gateway only |
| `make dev-notification` | Notification service only |
| `make dev-simulator` | Simulator service only |
| `make dev-lite` | Infra + payment service + frontend |
| `make dev` | Full development environment |

## Local Development Profiles

Services now support a `local` Spring profile that optimizes for development:
- Shorter timeouts and retry counts
- Failing fast circuit breaker settings
- Isolated Kafka topics (prefixed with `.local`)
- Disabled JPA auto-update (use migrations instead)
- Shorter idempotency TTL

To activate local profile explicitly:
```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run -pl src/payment-service
```

## Development Tips

### Hot Reload
Code changes trigger automatic restart when DevTools is configured.

### Debugging
Remote debugging available on port 5005:
```bash
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Testing
Run service-specific tests:
```bash
mvn test -pl src/payment-service
mvn test -pl src/notification-service
```

## Environment Variables

Key variables for local development:
- `POSTGRES_HOST=localhost`
- `POSTGRES_PORT=5432`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- `JWT_SECRET` (auto-generated if missing)

See `.env.example` for complete list.