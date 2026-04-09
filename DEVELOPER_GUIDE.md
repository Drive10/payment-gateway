# PayFlow Developer Guide

Welcome to PayFlow! This guide will help you get started with local development.

## Prerequisites

- Docker & Docker Compose
- Java 21+ (for backend)
- Node.js 20+ & npm (for frontend)
- Maven 3.9+ (for backend builds)
- IntelliJ IDEA (recommended for backend)
- VSCode (recommended for frontend)

## Quick Start

### 1. Start Infrastructure

```bash
# Terminal 1: Start Docker infrastructure
docker compose up -d
```

This starts: PostgreSQL, MongoDB, Redis, Kafka, Zipkin

### 2. Start Backend (IntelliJ)

Run each service from IntelliJ using Maven:

```bash
# Run services individually
mvn spring-boot:run -pl services/auth-service
mvn spring-boot:run -pl services/order-service
mvn spring-boot:run -pl services/payment-service
mvn spring-boot:run -pl services/notification-service
mvn spring-boot:run -pl services/simulator-service
mvn spring-boot:run -pl services/analytics-service
mvn spring-boot:run -pl services/audit-service
mvn spring-boot:run -pl services/api-gateway
```

Or create IntelliJ Run Configurations for each service.

### 3. Start Frontend (VSCode)

```bash
# Terminal 2: Start frontend
cd web/frontend
npm run dev
```

## Service Ports

| Service | Port |
|---------|------|
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Order Service | http://localhost:8082 |
| Payment Service | http://localhost:8083 |
| Notification | http://localhost:8084 |
| Simulator | http://localhost:8086 |
| Analytics | http://localhost:8089 |
| Audit Service | http://localhost:8090 |

## Infrastructure Ports

| Service | Port |
|---------|------|
| PostgreSQL | 5432 |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka | 9092 |
| Zipkin | 9411 |

## Common Commands

```bash
# Start infrastructure
docker compose up -d

# Stop infrastructure
docker compose down

# Start with fresh data
docker compose down -v
docker compose up -d

# Frontend
cd web/frontend
npm run dev      # Development
npm run build    # Production build

# Backend tests
mvn test -pl services/payment-service

# Frontend tests
cd web/frontend
npm test
```

## Environment Variables

Main configuration is in `.env` file. Key variables:

- `DB_HOST=localhost` - PostgreSQL
- `REDIS_HOST=localhost` - Redis
- `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` - Kafka

For frontend, create `web/frontend/.env`:
```
VITE_API_BASE_URL=http://localhost:8080
```

## Troubleshooting

### Port already in use
```bash
# Find process using port
lsof -i :8080
# Kill if needed
kill <PID>
```

### Database connection issues
```bash
# Check if PostgreSQL is running
docker compose ps postgres
# Restart infrastructure
docker compose restart
```

### Clear all data
```bash
docker compose down -v
docker compose up -d
```