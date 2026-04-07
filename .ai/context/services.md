# Service Context

> Detailed context for each PayFlow microservice

---

## 1. API Gateway (8080)

### Responsibility
- Route requests to appropriate services
- JWT validation and authentication
- Rate limiting (Redis-based)
- CORS policy enforcement
- Circuit breaker (Resilience4j)

### Key Endpoints
- `/api/v1/*` → Route to services
- `/actuator/health` → Health check

### Dependencies
- Redis (rate limiting)
- Auth Service (token validation)

---

## 2. Auth Service (8081)

### Responsibility
- User registration and login
- JWT token generation and refresh
- OAuth2 integration
- Session management
- RBAC (Admin, Merchant, User)

### Key Endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`

### Dependencies
- PostgreSQL (users, roles)
- Redis (sessions, token blacklist)

---

## 3. Order Service (8082)

### Responsibility
- Order creation and management
- Merchant management
- API key generation
- KYC (Know Your Customer)

### Key Endpoints
- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PUT /api/v1/orders/{id}`

### Dependencies
- PostgreSQL (orders, merchants)
- Payment Service (payment creation)

---

## 4. Payment Service (8083)

### Responsibility
- Payment orchestration
- Multi-provider integration (Stripe, Razorpay, PayPal)
- Payment processing and refunds
- Idempotency keys
- Webhook handling

### Key Endpoints
- `POST /api/v1/payments`
- `POST /api/v1/payments/{id}/capture`
- `POST /api/v1/payments/{id}/refunds`
- `GET /api/v1/payments`

### Dependencies
- PostgreSQL (payments)
- Redis (idempotency keys)
- Kafka (payment events)
- External providers (Stripe, Razorpay)

---

## 5. Notification Service (8084)

### Responsibility
- Email notifications
- SMS notifications
- Push notifications
- Webhook delivery
- Feature flags

### Key Endpoints
- `POST /api/v1/notifications/send`
- `GET /api/v1/webhooks`
- `POST /api/v1/webhooks`

### Dependencies
- Kafka (event consumption)
- Redis (feature flags)
- External (email/SMS providers)

---

## 6. Simulator Service (8086)

### Responsibility
- Payment simulation for testing
- Load testing support
- Mock payment providers

### Dependencies
- Kafka (test events)

---

## 7. GraphQL Gateway (8087)

### Responsibility
- GraphQL API with schema federation
- Real-time subscriptions (WebSocket)
- DataLoader for N+1 prevention

### Endpoints
- `/graphql` - GraphQL endpoint
- `/graphiql` - Playground

### Dependencies
- All services (federation)

---

## 8. Search Service (8088)

### Responsibility
- Full-text search (Elasticsearch)
- Payment and order search
- Aggregations and analytics

### Key Endpoints
- `GET /api/v1/search/payments`
- `GET /api/v1/search/orders`

### Dependencies
- Elasticsearch
- Kafka (index events)

---

## 9. Analytics Service (8089)

### Responsibility
- Revenue analytics
- Payment trends
- Merchant reports
- Risk scoring
- Settlements and disputes

### Key Endpoints
- `GET /api/v1/analytics/summary`
- `GET /api/v1/analytics/reports`

### Dependencies
- PostgreSQL
- Kafka

---

## 10. Audit Service (8089)

### Responsibility
- MongoDB audit logging
- Event sourcing
- Compliance tracking
- User activity logs

### Dependencies
- MongoDB

---

## Service Communication

### Sync (Feign Client)
- Auth → Order
- Order → Payment
- API Gateway → All services

### Async (Kafka)
- Payment → Notification
- Payment → Analytics
- Payment → Search
- Payment → Audit

---

## Port Mapping

| Service | HTTP Port | Health |
|---------|-----------|--------|
| api-gateway | 8080 | /actuator/health |
| auth-service | 8081 | /actuator/health |
| order-service | 8082 | /actuator/health |
| payment-service | 8083 | /actuator/health |
| notification-service | 8084 | /actuator/health |
| simulator-service | 8086 | /actuator/health |
| graphql-gateway | 8087 | /actuator/health |
| search-service | 8088 | /actuator/health |
| analytics-service | 8089 | /actuator/health |
| audit-service | 8089 | /actuator/health |