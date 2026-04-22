# PayFlow - Enterprise Payment Gateway

Cloud-native payment platform with Spring Boot microservices, Kafka, PostgreSQL, Redis, and React.

## Prerequisites

- Java 21+ | Maven 3.9+
- Docker + Docker Compose
- Node.js 20+

## Quick Start

### 1. Start Infrastructure
```bash
docker compose up -d
```

### 2. Start Backend Services
```bash
mvn spring-boot:run -pl src/api-gateway,src/auth-service,src/order-service,src/payment-service,src/notification-service,src/simulator-service
```

Or run individual services:
```bash
mvn spring-boot:run -pl src/auth-service
mvn spring-boot:run -pl src/order-service
mvn spring-boot:run -pl src/payment-service
```

### 3. Start Frontend
```bash
cd frontend/payment-page && npm install && npm run dev
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Routing, auth |
| Auth Service | 8081 | JWT, sessions |
| Order Service | 8082 | Orders |
| Payment Service | 8083 | Payments |
| Notification | 8084 | Notifications |
| Simulator | 8086 | Mock provider |
| Frontend | 5173 | Checkout UI |

## Common Tasks

### Build all services
```bash
mvn clean package -DskipTests
```

### Run tests
```bash
mvn test                        # Backend tests
cd frontend/payment-page && npm test  # Frontend tests
```

### View logs
```bash
docker compose logs -f [service]
```

### Stop infrastructure
```bash
docker compose down
```

## API Testing

```bash
# Register user
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123","firstName":"Test","lastName":"User"}'

# Login
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"pass123"}'

# Create payment
curl -X POST http://localhost:8083/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":100,"currency":"INR"}'
```

## Tech Stack

- Java 21 + Spring Boot 3.3
- PostgreSQL 16, Redis 7, Kafka 3.7
- React 18 + Vite + Tailwind
- Docker Compose

## License

MIT