# AI Agent Guidance - Payment Gateway Project

## Project Overview

This is a production-grade payment gateway microservices architecture. The project is designed to be deployed in enterprise environments with proper secrets management via HashiCorp Vault and CI/CD via Jenkins.

## Critical Rules

### 1. Never Commit Secrets
- NEVER commit `.env` files to version control
- NEVER hardcode passwords, API keys, or secrets in code
- NEVER log sensitive information (passwords, tokens, keys)
- All production secrets MUST come from Vault or Jenkins credentials

### 2. Configuration Priority
The application uses this configuration hierarchy:
1. Environment variables (highest priority)
2. Vault secrets (when `VAULT_ENABLED=true`)
3. Default values in `application.yml`

### 3. Service Communication
- Services communicate via internal DNS names (e.g., `auth-service`, `postgres`)
- Use `SPRING_PROFILES_ACTIVE=docker` for containerized deployments
- Internal APIs require `X-Internal-Secret` header for authentication

## Working with Services

### Adding a New Service

1. Create service directory structure:
```
services/new-service/
├── pom.xml
└── src/main/
    ├── java/dev/payment/newservice/
    │   └── NewServiceApplication.java
    └── resources/
        └── application.yml
```

2. Add Vault integration in `application.yml`:
```yaml
spring:
  config:
    import: optional:vault://secret/new-service

datasource:
  password: ${vault:db-password}
```

3. Add to docker-compose.yml

4. Add to Jenkinsfile build stages

### Database Configuration

Each service has its own database. Use these environment variables:
- `DB_HOST` - PostgreSQL host
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name
- `DB_USERNAME` - Database user
- `DB_PASSWORD` - Database password (from Vault)

### Adding Secrets to Vault

```bash
# Via curl
curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"data":{"db-password":"value","jwt-secret":"value"}}' \
  http://localhost:8200/v1/secret/data/service-name
```

## Common Tasks

### Running Tests
```bash
mvn test
```

### Building Docker Images
```bash
docker build --build-arg SERVICE_PATH=services/auth-service -f Dockerfile.build .
```

### Checking Service Logs
```bash
docker logs payment-gateway-auth-service-1
```

### Accessing Container Shell
```bash
docker exec -it payment-gateway-auth-service-1 sh
```

## Environment-Specific Configurations

### Development
- Use `SPRING_PROFILES_ACTIVE=dev`
- Vault disabled or local instance
- Debug logging enabled

### Production
- Use `SPRING_PROFILES_ACTIVE=prod`
- Vault required
- TLS enabled for all connections
- Rate limiting enforced

## API Security

### Authentication Flow
1. Client calls `/api/v1/auth/login` with credentials
2. Auth service returns JWT access + refresh tokens
3. Client includes JWT in `Authorization: Bearer <token>` header
4. API Gateway validates JWT before routing

### Internal API Access
Internal endpoints require header: `X-Internal-Secret: <value from Vault>`

## Troubleshooting

### Service Won't Start
1. Check database connection: `docker logs <service> | grep -i connect`
2. Verify Vault is accessible: `curl http://vault:8200/v1/sys/health`
3. Check environment variables: `docker exec <service> env | grep DB`

### Database Authentication Failed
1. Verify password in Vault matches PostgreSQL user password
2. Check PostgreSQL logs: `docker logs payment-gateway-postgres-1`
3. Ensure database user exists and has correct permissions

### JWT Decoding Error
1. Verify JWT secret in Vault is valid Base64 (no `-` or `/` characters, or use URL-safe Base64)
2. Check the secret matches across all services

## File Patterns to Avoid Editing

- `*.jar` - Never modify or add
- `.env` - Never commit to version control
- `target/` - Never commit build outputs

## CI/CD Pipeline

The Jenkins pipeline:
1. Loads secrets from Jenkins credentials (NOT .env files)
2. Builds all services with Maven
3. Runs unit tests
4. Performs security scan
5. Starts infrastructure (Vault, PostgreSQL, Redis, Kafka)
6. Initializes Vault with secrets from Jenkins
7. Builds and pushes Docker images
8. Deploys services
9. Runs health checks and smoke tests
