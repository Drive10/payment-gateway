# PayFlow Development Guide

## API Documentation
 
We provide a centralized OpenAPI/Swagger portal that aggregates documentation for all microservices.
 
- **Portal URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
 
From this portal, you can select any service from the dropdown menu to view its available endpoints, request schemas, and response types.
 
## Quick Start

### Full Development Environment
```bash
./start-dev.sh  # Starts infrastructure, builds, and runs all services + frontend
```

### Selective Service Development
When working on a specific service, you can start just what you need:

```bash
# Start infrastructure only
make infra-up

# Start auth service with local profile
make auth

# Start payment service with local profile
make payment

# Start frontend only
make frontend

# Start payment service + frontend (lightweight)
make dev-lite
```

## Available Development Targets

| Target | Description |
|--------|-------------|
| `make auth` | Auth service (8082) |
| `make payment` | Payment service (8083) |
| `make frontend` | Frontend development server |
| `make gateway` | API Gateway (8080) |
| `make notification` | Notification service (8085) |
| `make simulator` | Simulator service (8086) |
| `make analytics` | Analytics service (8087) |
| `make audit` | Audit service (8088) |
| `make dev-lite` | Infra + payment + frontend |
| `make all-services` | All microservices |
| `make dev` | Infra + all services + frontend |

## Local Development Profiles

Services support a `local` Spring profile that optimizes for development:
- Disabled Flyway (uses JPA ddl-auto=update instead)
- Localhost connections for PostgreSQL, Redis, Kafka
- Direct service URLs (no Docker internal hostnames)

To activate local profile explicitly:
```bash
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run -pl src/payment-service
```

## Service Ports

| Service | Port | Access | Auth |
|---------|------|--------|------|
| API Gateway | 8080 | **Public** | JWT |
| Auth Service | 8082 | Internal only | JWT |
| Payment Service | 8083 | Internal only | Internal Token |
| Notification | 8085 | Internal only | - |
| Simulator | 8086 | Internal only | - |
| Analytics | 8087 | Internal only | - |
| Audit | 8088 | Internal only | - |


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

### Testing
Run service-specific tests:
```bash
mvn test -pl src/payment-service
mvn test -pl src/auth-service
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