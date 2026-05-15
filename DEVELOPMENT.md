# PayFlow Development Guide

## API Documentation

Swagger UI available at http://localhost:8080/swagger-ui.html via the API Gateway, which aggregates routes to all service docs.

## Quick Start

### Full Stack (Docker)
```bash
docker compose --profile services up -d
# All 11 containers running in ~30s
```

### Selective Service Development

```bash
# Infrastructure only
docker compose --profile infra up -d

# Run a specific service via Maven (with hot reload)
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local

# Frontend
cd frontend/payment-page && npm run dev

# Dev targets
make infra-up       # Docker infra
make payment        # Payment service (Maven)
make frontend       # Frontend dev server
make dev-lite       # Infra + payment + frontend
make all-services   # All microservices via Maven
make dev            # Everything
```

## Available Targets

| Target | Description |
|--------|-------------|
| `make build` | Build all JARs |
| `make test` | Run backend tests |
| `make test-frontend` | Run frontend tests |
| `make test-payment-card` | Quick API card flow test |
| `make health` | Check all service health |
| `make shell` | Open PostgreSQL shell |
| `make docker-build` | Build Docker images |
| `make docker-up` | Start with Docker Compose |

## Local Development Profiles

Services support a `local` Spring profile (activated via `SPRING_PROFILES_ACTIVE=local`):
- Connects to localhost PostgreSQL, Redis, Kafka
- Flyway enabled for schema management
- Uses `application.yml` defaults

The `docker` profile (used via Docker Compose):
- Connects to service hostnames (postgres, redis, kafka)
- Flyway disabled, JPA `ddl-auto=update`
- Hardcoded dev secrets in `application-docker.yml`

## Hot Reload

```bash
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local
# Code changes trigger automatic restart
```

## Debugging

```bash
mvn spring-boot:run -pl src/payment-service \
  -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

## Testing

```bash
# All backend tests
mvn test

# Specific service
mvn test -pl src/payment-service

# Frontend
cd frontend/payment-page && npm test

# E2E (requires full stack)
cd frontend/payment-page && npm run test:e2e

# Coverage
make test-coverage
```

## Environment Variables

For local Maven runs, the base `application.yml` uses `${VAR:-default}` patterns. Key variables:

| Variable | Default | Service |
|----------|---------|---------|
| `POSTGRES_HOST` | localhost | all |
| `POSTGRES_PASSWORD` | (required) | auth, payment, analytics, audit |
| `REDIS_HOST` | localhost | auth, payment, gateway |
| `REDIS_PASSWORD` | (required) | auth, payment, gateway |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | payment, notification, simulator, analytics, audit |
| `JWT_SECRET` | (required) | auth, gateway |
| `INTERNAL_AUTH_SECRET` | (required) | auth, payment, gateway |
| `PAYMENT_WEBHOOK_SECRET` | (required) | payment, simulator |

For Docker runs, all these are set with dev defaults in `application-docker.yml` — no `.env` needed.

## Security Practices

- No secrets committed to version control
- Run pre-commit hooks: `pre-commit install && pre-commit run --all-files`
- Validate all user inputs
- Use parameterized queries
- Report security issues via [SECURITY.md](SECURITY.md)
