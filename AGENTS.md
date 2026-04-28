# PayFlow Development Guide

## Quick Start

```bash
# One command to start everything
./start-dev.sh
```

## Manual Start

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Build
mvn clean package -DskipTests

# 3. Start services with local profile
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local &
mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local &

# 4. Start frontend
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
- **Fix**: Update `frontend/payment-page/public/env.js`:
  ```javascript
  window.__ENV__ = {
    API_BASE_URL: "http://localhost:8083",  // Point to payment service directly
    IS_PRODUCTION: false
  };
  ```

## Application Ports
- Frontend: 5173
- Payment API: 8083
- Auth API: 8082
- API Gateway: 8080
- Simulator: 8086

## Test Payment Flow
```bash
# 1. Get API key from merchant
API_KEY=$(docker compose exec -T postgres psql -U payflow -d payflow -t -c "SELECT api_key FROM public.merchants LIMIT 1;" | xargs)

# 2. Create payment
curl -X POST http://localhost:8083/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d '{"orderId":"test123","amount":100,"currency":"USD","paymentMethod":"CARD"}'

# 3. Authorize pending → Authorize → Capture
```