# PayFlow Architectural Refactor Summary

## Overview

This document describes the architectural refactor of PayFlow payment gateway system to align with production-ready patterns used by Stripe and Razorpay.

---

## Architecture Diagram (Post-Refactor)

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│                              UNTRUSTED ZONE                                      │
│                                                                                │
│   ┌─────────────────┐          ┌──────────────────────────────────────────┐   │
│   │                 │          │                                          │   │
│   │   Frontend      │─────────▶│           API Gateway                     │   │
│   │                 │          │           (Port: 8080)                    │   │
│   │  Payment Page  │          │                                          │   │
│   └─────────────────┘          └──────────────────┬───────────────────────────┘   │
│                                                │                              │
│                                                │ /api/v1/merchant/*           │
│                                                ▼ (JWT required)              │
│                          ┌────────────────────────────────────────────┐        │
│                          │         Merchant Backend                   │        │
│                          │         (Port: 8081)                       │        │
│                          │                                            │        │
│                          │  • Validates frontend request (JWT)       │        │
│                          │  • Creates order snapshot                │        │
│                          │  • Calls payment service (API key)        │        │
│                          └──────────────────┬─────────────────────────┘        │
│                                             │                              │
│                                             │ /api/v1/payments/*           │
│                                             ▼ (API Key: Bearer sk_test_*)    │
│                          ┌─────────────────────────────────────────────┐       │
│                          │         Payment Service                      │       │
│                          │         (Port: 8083)                       │       │
│                          │                                            │       │
│                          │  • Payment lifecycle                       │       │
│                          │  • Idempotency                             │       │
│                          │  • Kafka/outbox                             │       │
│                          │  • Simulator integration                  │       │
│                          └─��────────────────┬──────────────────────────┘       │
└─────────────────────────────────────────────┼───────────────────────────────┘
                                             │
                                             │ Kafka
                                             ▼
                          ┌────────────────────────────────────────────┐
                          │   Kafka / Redis / PostgreSQL           │
                          │   (Shared Infrastructure)             │
                          └──────────────────┬────────────────────┘
                                             │
                                             ▼
                          ┌────────────────────────────────────┐
                          │   Simulator Service                 │
                          │   (Provider Behavior)               │
                          └────────────────────────────────────┘
```

---

## Trust Boundaries

| Component | Zone | Authentication | Can Call |
|-----------|------|---------------|---------|
| Frontend | UNTRUSTED | None | API Gateway only |
| API Gateway | BOUNDARY | JWT (merchant routes) | Merchant Backend |
| Merchant Backend | TRUSTED | JWT | Payment Service |
| Payment Service | TRUSTED | API Key | Simulator, Infrastructure |

---

## Updated API Contracts

### 1. Frontend → Merchant Backend

**Endpoint**: `POST /api/v1/merchant/create-payment`

**Request** (Frontend sends):
```json
{
  "productId": "prod_123"
}
```

- No amount, merchantId, or orderId in request
- Frontend is UNTRUSTED and cannot specify financial details

**Response**:
```json
{
  "success": true,
  "data": {
    "order": {
      "id": "order_abc123",
      "amount": 50000,
      "currency": "INR"
    },
    "payment": {
      "id": "pay_xyz789",
      "status": "CREATED",
      "checkoutUrl": "https://checkout.payflow.dev/pay/pay_xyz789"
    },
    "checkoutUrl": "https://checkout.payflow.dev/pay/pay_xyz789"
  }
}
```

### 2. Merchant Backend → Payment Service

**Endpoint**: `POST /api/v1/payments`

**Headers**:
```
Authorization: Bearer sk_test_merchant123
Idempotency-Key: idem_1234567890
X-Correlation-ID: corr_abc123
```

**Request**:
```json
{
  "order": {
    "id": "order_abc123",
    "amount": 50000,
    "currency": "INR"
  },
  "method": "CARD"
}
```

**Response**: Standard payment object

---

## Entity Models

### Merchant (DTO in common module)

```java
public record Merchant(
    UUID id,
    String apiKey,
    String status,       // "ACTIVE" or "BLOCKED"
    String webhookUrl,
    Instant createdAt
) {
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
```

### Order Snapshot (DTO in common module)

```java
public record OrderSnapshot(
    String id,          // order identifier from merchant system
    BigDecimal amount, // in smallest currency unit (paise for INR)
    String currency    // ISO 4217 currency code
) {}
```

### Payment (unchanged from existing)

- Maintains all existing fields
- Order relationship changed to snapshot (not owned internally)

---

## API Gateway Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: merchant-backend
          uri: ${MERCHANT_BACKEND_URL:http://localhost:8081}
          predicates:
            - Path=/api/v1/merchant/**
          filters:
            - StripPrefix=2
        - id: payment-service
          uri: ${PAYMENT_SERVICE_URL:http://localhost:8083}
          predicates:
            - Path=/api/v1/payments/**
          filters:
            - StripPrefix=2
```

### Filter Responsibilities

| Filter | Path | Responsibility |
|--------|------|----------------|
| JwtAuthenticationFilter | /api/v1/merchant/* | JWT validation |
| CorrelationIdFilter | All | Request tracking |
| RateLimitFilter | All | Rate limiting |

---

## Key Code Snippets

### 1. API Key Filter (Payment Service)

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String apiKey = authHeader.substring(7);
            
            if (apiKey.equals(validApiKey)) {
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(
                        "merchant",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                    );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Allow actuator, auth, internal endpoints
    }
}
```

### 2. Merchant Backend Controller

```java
@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody FrontendPaymentRequest request) {
        
        String customerEmail = getCurrentUserEmail();
        CreatePaymentResponse response = paymentService.createPayment(
            request.productId(),
            customerEmail
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 3. Payment Controller Changes

- Removed JWT authentication dependency
- Relies on API Key filter for authentication
- Accepts order snapshot instead of order ID

---

## Removed Components

| Component | Reason |
|-----------|--------|
| /api/v1/orders from frontend | Frontend cannot create orders directly |
| Order module in Payment Service | Orders owned by merchant, not payment service |
| Auth module in Payment Service | JWT moved to API Gateway + Merchant Backend |
| merchantId in API request | Replaced with API key-based merchant resolution |
| Hardcoded merchantId | Security vulnerability removed |

---

## Migration Plan

### Phase 1: Infrastructure (Completed)
1. Create merchant-backend service
2. Add API key authentication to payment service
3. Update common module DTOs

### Phase 2: Gateway (Completed)
1. Update routes configuration
2. Add merchant-backend route
3. Configure JWT for merchant routes only

### Phase 3: Payment Service (Completed)
1. Add API key filter
2. Update payment controller to support order snapshot
3. Keep idempotency, Kafka, simulator integration

### Phase 4: Frontend (Completed)
1. Update payment.ts to call merchant-backend
2. Remove direct /orders and /payments calls

### Phase 5: Verification (To Do)
1. Test end-to-end payment flow
2. Verify idempotency
3. Test webhook delivery

---

## Anti-Patterns Removed

| Anti-Pattern | Correct Pattern |
|-------------|-----------------|
| Frontend → /payments | Frontend → /merchant/create-payment |
| JWT for payment auth | API Key for payment service |
| merchantId in request | API key resolves merchant |
| Order created in payment | Order snapshot from merchant |
| Frontend specifies amount | Server-controlled amount |

---

## Preserved Strengths

| Feature | Status |
|--------|--------|
| Idempotency | ✓ Preserved |
| Kafka/Outbox | ✓ Preserved |
| State machine | ✓ Preserved |
| Simulator | ✓ Preserved |
| API Gateway | ✓ Preserved |

---

## Configuration Reference

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| MERCHANT_BACKEND_URL | Merchant service URL | http://localhost:8081 |
| PAYMENT_SERVICE_URL | Payment service URL | http://localhost:8083 |
| PAYMENT_SERVICE_API_KEY | API key for payment service | sk_test_merchant123 |
| JWT_SECRET | JWT signing key | (from config) |

### API Keys

Production keys follow format: `sk_live_...`
Test keys follow format: `sk_test_...`

---

## Testing Checklist

- [ ] Frontend can initiate payment via merchant-backend
- [ ] API key authentication works for payment service
- [ ] Idempotency prevents duplicate payments
- [ ] Correlation ID propagates through system
- [ ] Invalid API keys are rejected
- [ ] Simulator produces deterministic results
- [ ] Kafka events publish correctly

---

*Document generated as part of PayFlow architectural refactor*