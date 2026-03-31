# Payment Gateway - Testing Scenarios

## Overview

This document outlines the three testing scenarios for the payment gateway:
1. **Dev Mode** - Infrastructure in Docker, services run locally with Maven
2. **Docker Full** - All services run in Docker containers
3. **Hybrid Mode** - Mix of Docker and local services for edge case testing

---

## Prerequisites

- Docker Desktop (for infrastructure services)
- Java 21+
- Maven 3.9+
- curl or Postman (for API testing)

---

## Scenario 1: Dev Mode (Recommended for Development)

### Step 1: Start Infrastructure Services

```bash
# Start only infrastructure (PostgreSQL, Redis, Kafka)
docker compose --profile infra up -d
```

### Step 2: Verify Infrastructure

```bash
# Check PostgreSQL (port 5433)
docker compose exec postgres pg_isready -U payment

# Check Redis (port 6379)
docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" ping

# Check Kafka (port 9092)
docker compose exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Step 3: Build All Services

```bash
# From project root
mvn clean package -DskipTests
```

### Step 4: Run Services (in separate terminals)

```bash
# Terminal 1: Config Service
cd services/config-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 2: Auth Service
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 3: Payment Service
cd services/payment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4: Order Service
cd services/order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5: Notification Service
cd services/notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 6: Simulator Service
cd services/simulator-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 7: Webhook Service
cd services/webhook-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 8: Settlement Service
cd services/settlement-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 9: Risk Service
cd services/risk-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 10: Analytics Service
cd services/analytics-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 11: Merchant Service
cd services/merchant-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 12: Dispute Service
cd services/dispute-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 13: API Gateway (start last)
cd services/api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Step 5: Verify All Services

```bash
# Check health of all services
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Order Service
curl http://localhost:8083/actuator/health  # Payment Service
curl http://localhost:8084/actuator/health  # Notification Service
curl http://localhost:8085/actuator/health  # Webhook Service
curl http://localhost:8086/actuator/health  # Simulator Service
curl http://localhost:8087/actuator/health  # Settlement Service
curl http://localhost:8088/actuator/health  # Risk Service
curl http://localhost:8089/actuator/health  # Analytics Service
curl http://localhost:8090/actuator/health  # Merchant Service
curl http://localhost:8091/actuator/health  # Dispute Service
```

### Step 6: Run Integration Tests

```bash
# Run all integration tests
mvn verify -Dskip.unit.tests=true

# Run contract tests
mvn spring-cloud-contract:verify
```

### Step 7: Stop Infrastructure

```bash
# Stop and remove infrastructure containers
docker compose --profile infra down -v
```

---

## Scenario 2: Docker Full (Production-like Testing)

### Step 1: Build All Service Images

```bash
# Build all services
docker compose build
```

### Step 2: Start All Services

```bash
# Start everything (infrastructure + services + observability)
docker compose --profile services --profile observability up -d
```

### Step 3: Wait for Services to be Healthy

```bash
# Watch service health
watch docker compose ps
```

### Step 4: Verify All Services

```bash
# Check all services are running
docker compose ps

# Check logs if any service fails
docker compose logs <service-name>
```

### Step 5: Run E2E Tests

```bash
# Run integration tests against Docker services
mvn verify -Dskip.unit.tests=true -Dspring.profiles.active=docker

# Or use the API Gateway
curl http://localhost:8080/actuator/health
```

### Step 6: View Observability Stack

- **Grafana**: http://localhost:3002 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Jaeger**: http://localhost:16686

### Step 7: Stop All Services

```bash
# Stop everything
docker compose --profile services --profile observability down -v
```

---

## Scenario 3: Hybrid Mode (Edge Case Testing)

### Edge Case A: Infrastructure in Docker, Services Local

This tests services connecting to containerized infrastructure.

```bash
# Step 1: Start infrastructure in Docker
docker compose --profile infra up -d

# Step 2: Build services
mvn clean package -DskipTests

# Step 3: Run each service with dev profile (connects to Docker infra)
# Terminal 1: Auth Service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -pl services/auth-service

# Terminal 2: Payment Service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -pl services/payment-service

# ... repeat for other services

# Step 4: Test inter-service communication
# Services running locally should communicate with each other
# Docker infrastructure (Postgres, Redis, Kafka) should be accessible
```

### Edge Case B: Some Services in Docker, Some Local

This tests mixed deployment scenarios.

```bash
# Step 1: Start infrastructure in Docker
docker compose --profile infra up -d

# Step 2: Start some services in Docker
docker compose up -d auth-service notification-service webhook-service

# Step 3: Run remaining services locally
mvn spring-boot:run -pl services/payment-service -Dspring-boot.run.profiles=local
mvn spring-boot:run -pl services/order-service -Dspring-boot.run.profiles=local
mvn spring-boot:run -pl services/api-gateway -Dspring-boot.run.profiles=local

# Step 4: Test communication between Docker and local services
# Docker services use container names (e.g., http://auth-service:8081)
# Local services use localhost (e.g., http://localhost:8083)
# API Gateway must be configured to route correctly
```

### Edge Case C: All Local with Docker Infrastructure

This tests local development without containerizing services.

```bash
# Step 1: Start only Docker infrastructure
docker compose --profile infra up -d

# Step 2: Modify application-local.yml for each service to point to Docker infra
# Example: postgres host = localhost, port = 5433

# Step 3: Run services locally
mvn spring-boot:run -pl services/config-service
mvn spring-boot:run -pl services/auth-service
# ... etc
```

### Edge Case D: Network Partition Simulation

```bash
# Step 1: Start all services
docker compose --profile services up -d

# Step 2: Simulate network partition
docker network disconnect fintech-network payment-service

# Step 3: Test service behavior when partitioned
# Payment service should retry and eventually fail gracefully
# Other services should handle the failure

# Step 4: Reconnect
docker network connect fintech-network payment-service
```

### Edge Case E: Service Restart and Recovery

```bash
# Step 1: Start all services
docker compose --profile services up -d

# Step 2: Kill a service
docker compose kill payment-service

# Step 3: Test that dependent services handle the failure
# Order service should retry payment calls
# API Gateway should return appropriate errors

# Step 4: Restart the service
docker compose start payment-service

# Step 5: Verify recovery
curl http://localhost:8083/actuator/health
```

### Edge Case F: Database Failover

```bash
# Step 1: Start infrastructure
docker compose --profile infra up -d

# Step 2: Stop PostgreSQL
docker compose stop postgres

# Step 3: Test service behavior
# Services should fail to start or handle DB connection errors

# Step 4: Restart PostgreSQL
docker compose start postgres

# Step 5: Verify services recover
curl http://localhost:8081/actuator/health
```

### Edge Case G: Kafka Consumer Lag

```bash
# Step 1: Start infrastructure
docker compose --profile infra up -d

# Step 2: Start services except consumer
docker compose up -d auth-service payment-service

# Step 3: Produce messages to Kafka
# Use kafka-console-producer or send events via API

# Step 4: Start consumer service
docker compose up -d notification-service

# Step 5: Verify consumer catches up
# Check notification-service logs for processing
```

---

## Quick Test Script

```bash
#!/bin/bash
# quick-test.sh - Run all test scenarios

set -e

echo "=== Scenario 1: Dev Mode ==="
docker compose --profile infra up -d
sleep 30
echo "Infrastructure ready. Run services manually with: mvn spring-boot:run -pl services/<service>"
echo "Press Enter when ready to continue..."
read
docker compose --profile infra down -v

echo "=== Scenario 2: Docker Full ==="
docker compose --profile services up -d
sleep 120
echo "Checking services..."
curl -s http://localhost:8080/actuator/health || echo "API Gateway not ready"
docker compose --profile services down -v

echo "=== Scenario 3: Hybrid Mode ==="
docker compose --profile infra up -d
echo "Infrastructure started. Run some services locally and test."
echo "Press Enter to clean up..."
read
docker compose --profile infra down -v

echo "All scenarios completed!"
```

---

## Troubleshooting

### Port Conflicts

```bash
# Check what's using a port
netstat -ano | findstr :8080

# Kill process using a port
taskkill /PID <PID> /F
```

### Docker Issues

```bash
# Clean up Docker
docker system prune -a
docker volume prune

# Restart Docker Desktop
# ...
```

### Service Not Starting

```bash
# Check logs
docker compose logs <service>
mvn spring-boot:run -pl services/<service> --debug

# Check environment variables
docker compose exec <service> env | grep SPRING
```

---

## API Endpoints for Testing

### Authentication
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","firstName":"Test","lastName":"User"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### Payments
```bash
# Create payment
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"orderId":"order-123","amount":100.00,"currency":"USD"}'

# Get payment status
curl http://localhost:8080/api/payments/<payment-id> \
  -H "Authorization: Bearer <token>"
```

### Orders
```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"items":[{"productId":"prod-1","quantity":1,"price":99.99}]}'
```

---

## Performance Testing

```bash
# Install hey (HTTP benchmarking tool)
go install github.com/rakyll/hey@latest

# Test API Gateway
hey -n 1000 -c 10 http://localhost:8080/actuator/health

# Test Auth Service
hey -n 1000 -c 10 http://localhost:8081/actuator/health
```
