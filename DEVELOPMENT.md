# PayFlow Development Guide

## API Documentation
 
We provide a centralized OpenAPI/Swagger portal that aggregates documentation for all microservices.
 
- **Portal URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
 
From this portal, you can select any service from the dropdown menu to view its available endpoints, request schemas, and response types.
 
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

# Start merchant backend with local profile
make dev-merchant

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
| `make dev-merchant` | Merchant backend service |
| `make dev-payment` | Payment service with local dev profile |
| `make dev-frontend` | Frontend development server |
| `make dev-gateway` | API Gateway only |
| `make dev-notification` | Notification service only |
| `make dev-simulator` | Simulator service only |
| `make dev-lite` | Infra + merchant + payment + frontend |
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

Merchant backend:
```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run -pl src/merchant-backend
```

## Service Ports

| Service | Port | Access | Auth |
|---------|------|--------|------|
| API Gateway | 8080 | **Public** | JWT |
| Payment Service | 8083 | Internal only | Internal Token |
| Auth Service | 8082 | Internal only | JWT |
| Notification | 8084 | Internal only | - |
| Simulator | 8086 | Internal only | - |


## Security Development Practices

When developing for PayFlow, please follow these security practices:

- Never commit secrets, API keys, or credentials - use environment variables
- Run pre-commit checks locally: `pre-commit install` then `pre-commit run --all-files`
- Validate all user inputs to prevent injection attacks
- Use parameterized queries to prevent SQL injection
- Follow OWASP Top 10 guidelines for secure coding
- Report security concerns via the [Security Policy](SECURITY.md)

## Development Tips

### Hot Reload
Code changes trigger automatic restart when DevTools is configured.

### Debugging
Remote debugging available on port 5005:
```bash
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

Merchant backend debugging:
```bash
mvn spring-boot:run -pl src/merchant-backend -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006"
```

### Testing
Run service-specific tests:
```bash
mvn test -pl src/merchant-backend
mvn test -pl src/payment-service
mvn test -pl src/notification-service
```

## Environment Variables
 
We use a centralized configuration approach. All secrets and environment-specific values are managed in the root `.env` file, which is then injected into services via Docker Compose or manually for local Maven runs.
 
### Configuration Flow:
`.env` (Root) $\rightarrow$ `docker-compose.yml` $\rightarrow$ `${VARIABLE}` in `application.yml` $\rightarrow$ Spring Boot
 
### Key variables for local development:
- `POSTGRES_HOST=localhost`
- `POSTGRES_PORT=5432`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
- `JWT_SECRET` - **Required** - must be set
- `INTERNAL_AUTH_SECRET` - **Required** - internal service token signing key
- `PAYMENT_SERVICE_URL=http://localhost:8083`
- `MERCHANT_API_KEYS` - Comma-separated merchant API keys

See `.env.example` for the complete list of variables.