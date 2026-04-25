# PayFlow Docker Setup

This document describes how to run PayFlow services using Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Maven 3.9+ (for building JARs)

## Quick Start

### 1. Build all services

```bash
# Build common module first
cd common && mvn clean install -DskipTests

# Build all services
cd ../src
for service in api-gateway merchant-backend payment-service notification-service simulator-service analytics-service audit-service; do
    cd $service
    mvn clean package -DskipTests
    cd ..
done
```

### 2. Start infrastructure only

```bash
docker-compose --profile infra up -d
```

This starts: PostgreSQL, MongoDB, Redis, Kafka

### 3. Start application services

```bash
docker-compose --profile services up -d
```

This starts all microservices.

### 4. Start everything

```bash
docker-compose up -d
```

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Main entry point |
| merchant-backend | 8081 | Merchant API |
| payment-service | 8083 | Payment processing |
| notification-service | 8084 | Notifications |
| simulator-service | 8086 | Payment simulator |
| analytics-service | 8089 | Analytics |
| audit-service | 8090 | Audit logs |

## Infrastructure Ports

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Main database |
| MongoDB | 27017 | Audit database |
| Redis | 6379 | Caching |
| Kafka | 9092 | Message broker |

## Environment Variables
 
We use a centralized configuration approach. You can override any of the defaults below by creating a `.env` file in the root directory. Docker Compose will automatically load variables from this file.
 
### Common


| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_PROFILES_ACTIVE | docker | Spring profile |
| JWT_SECRET | (32 hex chars) | JWT signing key |

### PostgreSQL

| Variable | Default | Description |
|----------|---------|-------------|
| POSTGRES_HOST | postgres | Database host |
| POSTGRES_PORT | 5432 | Database port |
| POSTGRES_DB | payflow | Database name |
| POSTGRES_USER | payflow | Database user |
| POSTGRES_PASSWORD | payflow | Database password |

### Redis

| Variable | Default | Description |
|----------|---------|-------------|
| REDIS_HOST | redis | Redis host |
| REDIS_PORT | 6379 | Redis port |
| REDIS_PASSWORD | payflow | Redis password |

### Kafka

| Variable | Default | Description |
|----------|---------|-------------|
| KAFKA_BOOTSTRAP_SERVERS | kafka:29092 | Kafka brokers |

### MongoDB

| Variable | Default | Description |
|----------|---------|-------------|
| MONGODB_HOST | mongodb | MongoDB host |
| MONGODB_PORT | 27017 | MongoDB port |
| MONGODB_DATABASE | audit_db | Database name |
| MONGODB_USERNAME | audit | Username |
| MONGODB_PASSWORD | audit | Password |

## Service URLs

Internal service communication URLs (used in Docker):

| Service | URL |
|---------|-----|
| api-gateway | http://api-gateway:8080 |
| merchant-backend | http://merchant-backend:8081 |
| payment-service | http://payment-service:8083 |
| notification-service | http://notification-service:8084 |
| simulator-service | http://simulator-service:8086 |
| analytics-service | http://analytics-service:8089 |
| audit-service | http://audit-service:8090 |

## Docker Commands

```bash
# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f api-gateway

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Rebuild specific service
docker-compose build payment-service
docker-compose up -d payment-service
```

## Configuration Files

- `docker-compose.yml` - Main compose file with all services
- `src/*/Dockerfile` - Docker image for each service
- `src/*/src/main/resources/application-docker.yml` - Docker-specific config

## Health Checks

All services include health checks. Use `docker-compose ps` to verify status.

## Development

For local development with live code reloading, see [DEVELOPMENT.md](./DEVELOPMENT.md).
