# API Gateway - Agent Rules

> Specific guidelines for AI agents working on the api-gateway

---

## Service Overview

- **Port**: 8080
- **Dependencies**: Redis (rate limiting), Auth Service

---

## Key Responsibilities

1. Route requests to appropriate services
2. JWT validation and authentication
3. Rate limiting (Redis-based)
4. CORS policy enforcement
5. Circuit breaker (Resilience4j)

---

## Important Files

```
services/api-gateway/
├── src/main/java/com/payflow/gateway/
│   ├── config/RoutesConfig.java
│   ├── filter/JwtAuthenticationFilter.java
│   └── filter/RateLimitFilter.java
└── src/main/resources/application.yml
```

---

## Critical Rules

1. **Never log JWT tokens** - Security risk
2. **Always validate tokens** - Don't bypass auth
3. **Implement circuit breaker** - For downstream services

---

## Route Configuration

| Path | Service | Auth Required |
|------|---------|---------------|
| /api/v1/auth/* | auth-service | No |
| /api/v1/orders/* | order-service | Yes |
| /api/v1/payments/* | payment-service | Yes |
| /api/v1/notifications/* | notification-service | Yes |

---

## Related Docs

- [.ai/rules/backend.md](../../.ai/rules/backend.md)
- [.ai/rules/devops.md](../../.ai/rules/devops.md)
- [.ai/context/services.md](../../.ai/context/services.md)