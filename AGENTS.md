# PayFlow Development Guide

## Quick Start

```bash
# One command to start everything (requires Docker + Maven + Node)
./start-dev.sh
```

## Manual Start

```bash
# 1. Set up environment variables
cp .env.example .env
# Edit .env with actual secrets (defaults provided for dev)

# 2. Start infrastructure
docker compose up -d

# 3. Build
mvn clean package -DskipTests

# 4. Start all services with local profile
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/analytics-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/audit-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local &

# 5. Start frontend
cd frontend/payment-page && npm run dev
```

## Application Ports
| Service | Port | Access |
|---------|------|--------|
| Frontend | 5173 | Public |
| API Gateway | 8080 | Public (ingress) |
| Auth Service | 8082 | Internal |
| Payment Service | 8083 | Internal |
| Notification Service | 8085 | Internal |
| Simulator Service | 8086 | Internal |
| Analytics Service | 8087 | Internal |
| Audit Service | 8088 | Internal |

## Security Model

All payment endpoints require authentication. Access through API Gateway only.

### Authentication Flow

1. Login to get JWT:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"Password123"}'
```

2. Use token for API requests:
```bash
curl -X POST http://localhost:8080/api/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{"orderId":"test123","amount":100,"currency":"USD","paymentMethod":"CARD"}'
```

## Secrets Configuration

Required environment variables in `.env`:
```bash
JWT_SECRET=your-256-bit-secret-key
INTERNAL_AUTH_SECRET=internal-service-secret
POSTGRES_PASSWORD=your-db-password
REDIS_PASSWORD=your-redis-password
PAYMENT_WEBHOOK_SECRET=webhook-signing-secret
MERCHANT_API_KEYS=key1,key2  # Comma-separated
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## Makefile Targets

```bash
make infra-up        # Start Docker infrastructure (PostgreSQL, Redis, Kafka)
make infra-down      # Stop infrastructure
make build           # Build all JARs
make test            # Run backend tests
make all-services    # Start all microservices
make frontend        # Start frontend dev server
make dev             # Infrastructure + all services + frontend
make dev-lite        # Infrastructure + payment + frontend
```
