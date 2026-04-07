# PayFlow API Reference

> Complete API documentation for PayFlow payment gateway

---

## Authentication

### Register
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "demo@payflow.dev",
  "password": "Demo@1234",
  "firstName": "Demo",
  "lastName": "User"
}
```

### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@payflow.com",
  "password": "Test@1234"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600,
  "tokenType": "Bearer"
}
```

### Refresh Token
```http
POST /api/v1/auth/refresh
Authorization: Bearer <refresh_token>
```

### Get Current User
```http
GET /api/v1/auth/me
Authorization: Bearer <access_token>
```

---

## Orders

### Create Order
```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 5000,
  "currency": "USD",
  "customerEmail": "test@example.com",
  "description": "Test Order",
  "metadata": {
    "orderType": "physical"
  }
}
```

### Get Orders
```http
GET /api/v1/orders?page=0&size=20
Authorization: Bearer <token>
```

### Get Order by ID
```http
GET /api/v1/orders/{orderId}
Authorization: Bearer <token>
```

---

## Payments

### Create Payment
```http
POST /api/v1/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "orderId": "ord_123",
  "provider": "STRIPE",
  "paymentMethod": "CARD",
  "amount": 5000,
  "currency": "USD",
  "returnUrl": "https://example.com/return",
  "metadata": {}
}
```

**Response:**
```json
{
  "id": "pay_abc123",
  "status": "PENDING",
  "clientSecret": "pi_xxx_secret_xxx",
  "paymentUrl": "https://checkout.stripe.com/...",
  "expiresAt": "2026-04-07T18:00:00Z"
}
```

### Get Payment
```http
GET /api/v1/payments/{paymentId}
Authorization: Bearer <token>
```

### Capture Payment
```http
POST /api/v1/payments/{paymentId}/capture
Authorization: Bearer <token>
```

### Refund Payment
```http
POST /api/v1/payments/{paymentId}/refunds
Authorization: Bearer <token>
Content-Type: application/json

{
  "amount": 2500,
  "reason": "customer_request"
}
```

### List Payments
```http
GET /api/v1/payments?page=0&size=20&status=COMPLETED
Authorization: Bearer <token>
```

---

## Payment Methods

### Available Providers
- `STRIPE` - Stripe payments
- `RAZORPAY` - Razorpay payments
- `PAYPAL` - PayPal payments
- `SIMULATOR` - Test simulator

### Payment Method Types
- `CARD` - Credit/Debit card
- `UPI` - Unified Payments Interface (India)
- `WALLET` - Digital wallets
- `BANK_TRANSFER` - Bank transfers

---

## Webhooks

### Payment Events
```json
{
  "event": "payment.completed",
  "timestamp": "2026-04-07T12:00:00Z",
  "data": {
    "paymentId": "pay_abc123",
    "orderId": "ord_123",
    "amount": 5000,
    "currency": "USD",
    "status": "COMPLETED"
  }
}
```

### Event Types
- `payment.created` - Payment initiated
- `payment.pending` - Payment awaiting confirmation
- `payment.completed` - Payment successful
- `payment.failed` - Payment failed
- `payment.refunded` - Payment refunded
- `payment.captured` - Payment captured
- `payment.expired` - Payment expired

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid payment method",
  "details": [
    {
      "field": "paymentMethod",
      "message": "Must be one of: CARD, UPI, WALLET"
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid or expired token"
}
```

### 403 Forbidden
```json
{
  "error": "FORBIDDEN",
  "message": "Insufficient permissions"
}
```

### 429 Too Many Requests
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Try again later.",
  "retryAfter": 60
}
```

### 500 Internal Server Error
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred"
}
```

---

## Idempotency

Include `Idempotency-Key` header for idempotent requests:
```http
POST /api/v1/payments
Idempotency-Key: unique-key-123
Authorization: Bearer <token>
```

---

## Rate Limits

| Endpoint | Limit |
|----------|-------|
| General | 1000 req/min |
| Payments | 100 req/min |
| Auth | 10 req/min |

---

## GraphQL API

Access GraphQL at `http://localhost:8087/graphql`

### Example Queries

```graphql
# Get payments
query {
  payments(page: 0, pageSize: 10) {
    payments {
      id
      amount
      status
      createdAt
    }
    totalCount
  }
}

# Get analytics summary
query {
  analyticsSummary(startDate: "2026-01-01", endDate: "2026-04-01") {
    totalTransactions
    totalVolume
    successRate
    averageAmount
  }
}

# Create payment intent
mutation {
  createPaymentIntent(input: {
    orderId: "ord_123"
    amount: 5000
    currency: "USD"
    provider: STRIPE
  }) {
    id
    clientSecret
    status
  }
}
```