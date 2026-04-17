# PayFlow Quick Start Guide

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker & Docker Compose

## Quick Start

### Option 1: Full Docker (Recommended for beginners)

```bash
# Start everything in Docker
make docker-up

# View logs
make logs

# Stop everything
make docker-down
```

### Option 2: Local Development (For contributors)

```bash
# 1. Start infrastructure only
make infra-up

# 2. Start all backend services
make dev

# Or start a single service
make dev-single S=payment-service

# 3. Start frontend (separate terminal)
cd frontend/payment-page && npm run dev
```

## Common Commands

| Command | Description |
|---------|-------------|
| `make docker-up` | Start full stack in Docker |
| `make infra-up` | Start only infra (DB, Redis, Kafka) |
| `make dev` | Start backend locally |
| `make dev-single S=auth-service` | Start single service |
| `make build` | Build all services |
| `make build-single S=payment-service` | Build single service |
| `make test` | Run all tests |
| `make test-single S=payment-service` | Test single service |
| `make lint` | Run linters |
| `make format` | Format code |
| `make health` | Check service health |
| `make clean` | Stop Docker and remove volumes |

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |
| Order Service | 8082 | http://localhost:8082 |
| Payment Service | 8083 | http://localhost:8083 |
| Notification Service | 8084 | http://localhost:8084 |
| Simulator Service | 8086 | http://localhost:8086 |
| Frontend | 5173 | http://localhost:5173 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

## IDE Setup

### VS Code
Open in VS Code and select "Reopen in Container" when prompted, or:
```bash
code .
# Press F1 -> "Dev Containers: Reopen in Container"
```

### IntelliJ IDEA
1. Import project as Maven
2. Set JDK to Java 21
3. Run services using Spring Boot Dashboard

## Troubleshooting

### Services won't start
```bash
# Check if infra is running
make ps

# Restart infra
make infra-down && make infra-up
```

### Port conflicts
```bash
# Kill process on port
lsof -ti:8083 | xargs kill -9
```

### Clear all data
```bash
make clean
```

## Development Workflow

1. Create branch: `git checkout -b feature/my-feature`
2. Make changes
3. Run tests: `make test-single S=payment-service`
4. Format code: `make format`
5. Commit and push
6. Create PR