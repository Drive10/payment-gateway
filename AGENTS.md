# PayFlow Development Guide

## Quick Start

```bash
# One command to start everything
./start-dev.sh
```

## Manual Start

```bash
# 1. Set up environment variables
cp .env.example .env
# Edit .env with actual secrets

# 2. Start infrastructure
docker compose up -d

# 3. Build
mvn clean package -DskipTests

# 4. Start services with local profile
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local &

# 5. Start frontend
cd frontend/payment-page && npm run dev
```

## Known Issues & Fixes

### Flyway + PostgreSQL Compatibility
- **Issue**: Flyway 10.15.0 doesn't support PostgreSQL 14.x/15.x/16.x
- **Fix**: Disable Flyway in `application-local.yml`:
  ```yaml
  spring:
    flyway:
      enabled: false
  ```

### Auth Service Bean Conflict
- **Issue**: Duplicate `passwordEncoder` bean
- **Fix**: Add to `application-local.yml`:
  ```yaml
  spring:
    main:
      allow-bean-definition-overriding: true
  ```

### Docker Compose Issues
- **Issue**: sample-data.sql fails during init
- **Fix**: Remove `- ./config/sample-data.sql:/docker-entrypoint-initdb.d/sample-data.sql` from docker-compose.yml
- Use PostgreSQL 14 (not 15/16)

### Frontend CORS Issue
- **Issue**: Frontend at localhost:5173 cannot reach backend
- **Fix**: Point to gateway:
  ```javascript
  window.__ENV__ = {
    API_BASE_URL: "http://localhost:8080",  // Point to gateway
    IS_PRODUCTION: false
  };
  ```

## Application Ports
- Frontend: 5173
- Payment API: 8083 **(**internal only**)**
- Auth API: 8082 **(**internal only**)**
- API Gateway: 8080 **(**public ingress**)**
- Simulator: 8086 **(**internal only**)**

## Security Model

All payment endpoints now require authentication. Access through API Gateway only.

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