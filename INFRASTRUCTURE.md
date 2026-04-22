# PayFlow Infrastructure & Run Guide

## 📁 Project Structure

```
payflow/
├── src/                      # Microservices (Java Spring Boot)
│   ├── api-gateway/          # Spring Cloud Gateway
│   ├── auth-service/         # Authentication & JWT
│   ├── order-service/       # Order management
│   ├── payment-service/     # Payment orchestration
│   ├── notification-service/ # Email/SMS/webhooks
│   ├── simulator-service/    # Payment simulation
│   ├── analytics-service/  # Analytics
│   └── audit-service/       # Audit logging
├── libs/                    # Shared libraries
│   ├── common/             # Common DTOs, utilities
│   └── common-config/       # Shared Spring config
├── frontend/               # React applications
│   └── payment-page/       # Checkout UI
├── config/                 # Configuration files
├── scripts/               # Run scripts
│   ├── dev.sh            # Local development
│   ├── docker.sh        # Full Docker
│   └── clean.sh         # Cleanup
├── docker-compose*.yml    # Docker Compose files
├── .env                  # Local environment
└── .env.docker          # Docker environment
```

### Why This Structure?

| Directory | Purpose |
|-----------|---------|
| `src/` | Each microservice is独立的Maven模块 |
| `libs/` | Shared code to avoid duplication |
| `frontend/` | React apps separate from backend |
| `config/` | K8s, Helm configs |
| `scripts/` | Developer experience scripts |

---

## 🐳 Docker Compose Strategy

### Layered Approach

| File | Purpose | Use Case |
|------|---------|----------|
| `docker-compose.base.yml` | Only infra (DB, Redis, Kafka) | Base for other compose files |
| `docker-compose.dev.yml` | Infra only for local dev | `make local` |
| `docker-compose.full.yml` | All services in Docker | Integration testing, CI |

### How Layering Works

```bash
# Infrastructure only
docker compose -f docker-compose.dev.yml up -d

# Full stack
docker compose -f docker-compose.full.yml up -d
```

**Benefits:**
- Single source of truth for infra config
- No duplication between modes
- Easy to add new services to full stack
- Clear separation of concerns

---

## 🌍 Environment Configuration

### Two Environment Files

| File | When to Use | Key Difference |
|------|------------|---------------|
| `.env` | Local development | `localhost` for services |
| `.env.docker` | Docker containers | Service hostnames |

### Key Variables

```bash
# Infrastructure
POSTGRES_HOST=localhost      # Use postgres in Docker
REDIS_HOST=localhost       # Use redis in Docker
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Service URLs
PAYMENT_SERVICE_URL=http://localhost:8083
NOTIFICATION_SERVICE_URL=http://localhost:8084
```

### Why No Hardcoded Hostnames

| Environment | POSTGRES_HOST | REDIS_HOST |
|------------|--------------|-----------|
| Local | `localhost` | `localhost` |
| Docker | `postgres` | `redis` |
| Prod (K8s) | `postgres.payflow.svc.cluster.local` | `redis.payflow.svc.cluster.local` |

All services use env vars - no code changes needed between environments.

---

## 🔧 Spring Boot Profiles

### Profile Structure

```
application.yml          # Base config (all environments)
application-local.yml   # Local development
application-docker.yml  # Docker containers
```

### How Profiles Work

```bash
# Local
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Docker (set via SPRING_PROFILES_ACTIVE in compose)
SPRING_PROFILES_ACTIVE=docker
```

### Configuration Inheritance

```yaml
# application.yml (base)
spring:
  config:
    import: optional:file:./application-${SPRING_PROFILES_ACTIVE:default}.yml

# application-local.yml (overrides for local)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/...
```

---

## 🚀 Running the Application

### Option 1: Local Development (Fast Iteration)

```bash
# Start infra in Docker, services locally
./scripts/dev.sh start

# Or use Makefile
make local
```

**What happens:**
1. PostgreSQL, Redis, Kafka start in Docker
2. All backend services start locally with Maven
3. Frontend starts with Vite
4. Services connect to Docker infra

**URLs:**
- API Gateway: http://localhost:8080
- Frontend: http://localhost:5173

### Option 2: Full Docker (Integration Testing)

```bash
# Start everything in Docker
./scripts/docker.sh start

# Or use Makefile
make docker
```

**What happens:**
1. All services build as Docker images
2. Infra + services run in containers
3. Services use Docker networking

### Cleanup

```bash
# Everything (Docker + local)
./scripts/clean.sh all

# Docker only
./scripts/clean.sh docker

# Local only
./scripts/clean.sh local
```

---

## 🌐 Networking Strategy

### Local Mode

```
┌─────────────────────────────────────┐
│         Developer Machine            │
│  ┌──────────┐   ┌──────────────┐   │
│  │ Services │   │ Docker Infra │   │
│  │ (Maven)  │───│ PostgreSQL  │   │
│  │          │   │ Redis       │   │
│  │          │   │ Kafka      │   │
│  └──────────┘   └──────────────┘   │
│        localhost:8080-8086          │
└─────────────────────────────────────┘
```

**Connection:** Services → `localhost:5432` (PostgreSQL)

### Docker Mode

```
┌─────────────────────────────────────────┐
│        Docker Network (payflow-network)   │
│  ┌────────────┐  ┌────────────────┐    │
│  │ Services  │  │   Infra       │    │
│  │           │  │               │    │
│  │ api-gw    │──│ postgres:5432 │    │
│  │ auth      │  │ redis:6379    │    │
│  │ payment   │  │ kafka:9092   │    │
│  └────────────┘  └────────────────┘    │
│         localhost:8080-8086              │
└─────────────────────────────────────────┘
```

**Connection:** Services → `postgres:5432` (Docker DNS)

### KeyNetworking Rules

| Rule | Why |
|------|-----|
| Use service names as hostnames in Docker | Docker provides DNS resolution |
| Use localhost for local dev | Services run alongside infra |
| Never hardcode `localhost` in configs | Fails in Docker |
| Always use `${VAR}` for hostnames | Environment-driven |

---

## 🗄️ Database Migration Strategy

### Current Setup (Hibernate)

```yaml
# application-local.yml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # Dev only!
```

### Recommended Production Pattern

Using Flyway for migrations:

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Migration Files Location

```
src/{service}/src/main/resources/
├── db/migration/
│   ├── V1__init_schema.sql
│   ├── V2__add_users.sql
│   └── ...
└── application.yml
```

### Migration Strategy

| Environment | Strategy |
|------------|----------|
| Local Dev | `ddl-auto: update` (dev convenience) |
| Docker | Flyway migration |
| CI/CD | Flyway migration |
| Production | Flyway migration only |

---

## 🔄 CI/CD Integration

### Docker Image Build

```bash
# Build all services
docker compose -f docker-compose.full.yml build

# Or for CI
./scripts/docker.sh build
```

### Integration Tests

```bash
# Start stack
docker compose -f docker-compose.full.yml up -d

# Run tests
mvn verify

# Cleanup
docker compose -f docker-compose.full.yml down
```

### Kubernetes Readiness

The structure is ready for K8s:

- All configs use environment variables
- Health checks defined in compose
- Services expose actuator endpoints
- No hardcoded values

---

## ⚠️ Common Failure Scenarios

### 1. "Works locally but not in Docker"

**Cause:** Using `localhost` in Docker networking

**Fix:** Use service names:
```yaml
# Wrong
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/payflow

# Correct
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payflow
```

### 2. Port Conflicts

**Cause:** Multiple services trying to bind same port

**Fix:** Ensure unique ports in configs:
```yaml
ports:
  - "8083:8083"  # payment-service
```

### 3. Kafka Connection Issues

**Cause:** Wrong `KAFKA_ADVERTISED_LISTENERS`

**Fix:** The compose files already handle this:
```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
```

### 4. Environment Variable Mismatch

**Cause:** Inconsistent env var names

**Fix:** Use consistent naming:
```bash
# All use same pattern
POSTGRES_HOST
REDIS_HOST
KAFKA_BOOTSTRAP_SERVERS
```

---

## 📋 Quick Reference

| Command | Description |
|---------|-------------|
| `./scripts/dev.sh start` | Local development |
| `./scripts/dev.sh stop` | Stop local dev |
| `./scripts/docker.sh start` | Full Docker |
| `./scripts/docker.sh stop` | Stop Docker |
| `./scripts/clean.sh all` | Cleanup everything |
| `make local` | Alias for dev.sh start |
| `make docker` | Alias for docker.sh start |

### Service Ports

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Auth | 8081 |
| Order | 8082 |
| Payment | 8083 |
| Notification | 8084 |
| Simulator | 8086 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |