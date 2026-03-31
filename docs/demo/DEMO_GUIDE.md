# Payment Gateway Demo Guide

A comprehensive walkthrough of the fintech payment gateway system.

## Prerequisites

1. Docker Desktop installed and running
2. At least 8GB RAM available
3. Ports 8080-8092, 3000-3002, 9090 available

## Quick Start (One-Command)

```bash
# Clone and start everything
git clone https://github.com/your-org/payment-gateway.git
cd payment-gateway
docker compose --profile services up --build
```

**Expected startup time:** 3-5 minutes

---

## Demo Scenarios

### Scenario 1: Successful Payment Flow

#### Step 1: Register a User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "Demo123!",
    "name": "Demo User"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "uuid",
    "email": "demo@example.com"
  }
}
```

#### Step 2: Login and Get JWT Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "Demo123!"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 86400
  }
}
```

#### Step 3: Create an Order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "description": "Demo Order - Premium Subscription"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "orderId": "uuid",
    "status": "PENDING",
    "amount": 5000,
    "currency": "USD"
  }
}
```

#### Step 4: Process Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: unique-key-12345" \
  -d '{
    "orderId": "ORDER_UUID",
    "amount": 5000,
    "currency": "USD",
    "paymentMethod": {
      "type": "card",
      "card": {
        "number": "4242424242424242",
        "expiryMonth": "12",
        "expiryYear": "25",
        "cvv": "123",
        "cardholderName": "Demo User"
      }
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "status": "AUTHORIZED",
    "amount": 5000,
    "currency": "USD"
  }
}
```

#### Step 5: Capture Payment

```bash
curl -X POST http://localhost:8080/api/v1/payments/PAYMENT_UUID/capture \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "status": "CAPTURED",
    "capturedAmount": 5000
  }
}
```

---

### Scenario 2: Failed Payment with Retry

#### Step 1: Attempt Payment with Invalid Card

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: retry-key-001" \
  -d '{
    "amount": 10000,
    "currency": "USD",
    "paymentMethod": {
      "type": "card",
      "card": {
        "number": "4000000000000002",
        "expiryMonth": "12",
        "expiryYear": "25",
        "cvv": "123"
      }
    }
  }'
```

**Response (Declined):**
```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_DECLINED",
    "message": "Card declined by issuer"
  }
}
```

#### Step 2: Retry with Different Card

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: retry-key-002" \
  -d '{
    "amount": 10000,
    "currency": "USD",
    "paymentMethod": {
      "type": "card",
      "card": {
        "number": "4242424242424242",
        "expiryMonth": "12",
        "expiryYear": "25",
        "cvv": "123"
      }
    }
  }'
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "paymentId": "uuid",
    "status": "AUTHORIZED"
  }
}
```

---

### Scenario 3: Refund Flow

#### Step 1: Create and Capture Payment

```bash
# Create payment (from Scenario 1)
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: refund-demo-001" \
  -d '{
    "amount": 5000,
    "currency": "USD",
    "paymentMethod": {
      "type": "card",
      "card": {
        "number": "4242424242424242",
        "expiryMonth": "12",
        "expiryYear": "25",
        "cvv": "123"
      }
    }
  }'

# Capture payment
curl -X POST http://localhost:8080/api/v1/payments/PAYMENT_UUID/capture \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Step 2: Process Full Refund

```bash
curl -X POST http://localhost:8080/api/v1/payments/PAYMENT_UUID/refunds \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "amount": 5000,
    "reason": "CUSTOMER_REQUEST"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "refundId": "uuid",
    "status": "COMPLETED",
    "amount": 5000
  }
}
```

#### Step 3: Process Partial Refund

```bash
curl -X POST http://localhost:8080/api/v1/payments/PAYMENT_UUID/refunds \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "amount": 1000,
    "reason": "PARTIAL_ITEM_RETURN"
  }'
```

---

### Scenario 4: Webhook Retry Simulation

#### Step 1: Check Webhook Delivery Logs

```bash
curl -X GET http://localhost:8085/api/v1/webhooks/deliveries \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Step 2: Trigger Manual Webhook Retry

```bash
curl -X POST http://localhost:8085/api/v1/webhooks/deliveries/DELIVERY_ID/retry \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

#### Step 3: View Dead Letter Queue

```bash
curl -X GET http://localhost:8085/api/v1/webhooks/dead-letter \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

### Scenario 5: Circuit Breaker Demonstration

This scenario demonstrates how the system handles downstream service failures.

```bash
# Enable failure simulation
curl -X POST http://localhost:8086/api/v1/simulator/config \
  -H "Content-Type: application/json" \
  -d '{
    "failureRate": 1.0,
    "delayMs": 5000
  }'

# Make multiple requests - first few will succeed, then circuit opens
for i in {1..15}; do
  curl -X POST http://localhost:8080/api/v1/payments \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
    -H "Idempotency-Key: cb-test-$i" \
    -d '{"amount": 100, "currency": "USD", "paymentMethod": {"type": "card", "card": {"number": "4242424242424242", "expiryMonth": "12", "expiryYear": "25", "cvv": "123"}}}'
  sleep 1
done

# Check circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers
```

**Expected behavior:**
- First ~10 requests will attempt to process
- Circuit breaker will open after threshold failures
- Subsequent requests return immediately with fallback response
- After wait duration, circuit enters half-open state
- Successful request closes circuit

---

### Scenario 6: Multi-Service Transaction

```bash
# Step 1: Register Merchant
curl -X POST http://localhost:8090/api/v1/merchants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo Store",
    "email": "merchant@demostore.com",
    "businessType": "RETAIL"
  }'

# Step 2: Create Order
curl -X POST http://localhost:8082/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "merchantId": "MERCHANT_UUID",
    "amount": 15000,
    "currency": "USD",
    "items": [
      {"name": "Product A", "quantity": 2, "price": 5000},
      {"name": "Product B", "quantity": 1, "price": 5000}
    ]
  }'

# Step 3: Process Payment
# (Same as Scenario 1)

# Step 4: View Settlement
curl -X GET http://localhost:8087/api/v1/settlements \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Observability Tools

### Grafana Dashboard

Access: http://localhost:3002

1. Default credentials: `admin` / `admin123`
2. Navigate to **Dashboards → Payment Gateway**
3. View real-time metrics:
   - Payment success rate
   - Response times
   - Active payments
   - Error rates

### Jaeger Tracing

Access: http://localhost:16686

1. Select service: `payment-service`
2. Click **Find Traces**
3. Filter by operation: `POST /api/v1/payments`
4. Click any trace to see detailed spans:
   - Gateway processing time
   - Auth validation
   - Database queries
   - External API calls
   - Kafka publish time

### Loki Log Explorer

Access: http://localhost:3002 (via Grafana)

1. Go to **Explore**
2. Select **Loki** datasource
3. Query logs:
   ```
   {app="payment-service"} |= "ERROR"
   {app="payment-service"} |= "correlationId=YOUR_ID"
   ```

---

## Troubleshooting

### Services Not Starting

```bash
# Check Docker logs
docker compose logs -f postgres
docker compose logs -f kafka
docker compose logs -f api-gateway

# Restart all services
docker compose restart
```

### Database Connection Issues

```bash
# Check if postgres is healthy
docker compose ps postgres

# Connect to postgres
docker compose exec postgres psql -U payment -d paymentdb
```

### Kafka Topics Not Created

```bash
# Create topics manually
docker compose exec kafka kafka-topics.sh \
  --create \
  --topic payment.events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1
```

### Out of Memory

```bash
# Increase Docker memory to 8GB minimum
# Or reduce services started:
docker compose --profile services up postgres kafka redis api-gateway
```

---

## Demo Completion Checklist

- [ ] All services healthy (check `docker compose ps`)
- [ ] Can register and login user
- [ ] Can create and capture payment
- [ ] Can process refund
- [ ] Grafana shows metrics
- [ ] Jaeger shows traces
- [ ] Understand the payment lifecycle
