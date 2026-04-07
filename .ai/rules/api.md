# API Design Rules

> REST and GraphQL standards for PayFlow

---

## 1. REST API Standards

### URL Structure

```
/api/v1/{resource}/{id}/{sub-resource}
```

| Method | Usage | Idempotent |
|--------|-------|------------|
| GET | Retrieve | Yes |
| POST | Create | No |
| PUT | Replace | Yes |
| PATCH | Update | No |
| DELETE | Remove | Yes |

### Examples

```
GET    /api/v1/payments              # List payments
POST   /api/v1/payments              # Create payment
GET    /api/v1/payments/{id}        # Get payment
PUT    /api/v1/payments/{id}         # Replace payment
PATCH  /api/v1/payments/{id}         # Update payment
DELETE /api/v1/payments/{id}         # Delete payment
POST   /api/v1/payments/{id}/refunds # Refund (sub-resource)
```

---

## 2. Response Format

### Success Response

```json
{
  "success": true,
  "data": { },
  "message": "optional message",
  "timestamp": "2026-04-07T12:00:00Z",
  "traceId": "abc123"
}
```

### List Response (Pagination)

```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  },
  "timestamp": "2026-04-07T12:00:00Z",
  "traceId": "abc123"
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_DECLINED",
    "message": "Card was declined",
    "field": "cardNumber",
    "details": []
  },
  "timestamp": "2026-04-07T12:00:00Z",
  "traceId": "abc123"
}
```

---

## 3. HTTP Status Codes

| Code | Usage |
|------|-------|
| 200 | OK - Success |
| 201 | Created - Resource created |
| 204 | No Content - Success, no response body |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing/invalid auth |
| 403 | Forbidden - No permission |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Duplicate or state conflict |
| 422 | Unprocessable - Validation failed |
| 429 | Too Many Requests - Rate limited |
| 500 | Internal Error - Server issue |
| 502 | Bad Gateway - Upstream error |
| 503 | Unavailable - Service down |
| 504 | Timeout - Upstream timeout |

---

## 4. Versioning

- Version in URL: `/api/v1/`
- Support versions for 12 months minimum
- Deprecate with warning headers
- Add to header: `Accept: application/vnd.payflow.v1+json`

---

## 5. GraphQL Standards

### Schema-First

- Define schema before implementation
- Use meaningful type names
- Implement proper resolvers

### Query Structure

```graphql
type Query {
    payments(filter: PaymentFilter, pagination: Pagination): PaymentConnection!
    payment(id: ID!): Payment
}

type PaymentConnection {
    edges: [PaymentEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type Mutation {
    createPayment(input: CreatePaymentInput!): PaymentPayload!
    refundPayment(id: ID!, input: RefundInput!): RefundPayload!
}
```

### N+1 Prevention (DataLoader)

```java
@DataLoader
public class PaymentDataLoader implements BatchLoader<String, Payment> {
    @Override
    public CompletionStage<List<Payment>> load(List<String> ids) {
        return CompletableFuture.supplyAsync(() -> 
            paymentRepository.findByIdIn(ids)
        );
    }
}
```

### Max Query Depth: 10

---

## 6. Rate Limiting

### Headers

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1712486400
```

### Limits

| Type | Limit |
|------|-------|
| Per user | 1000 req/min |
| Per IP | 500 req/min |
| Per API key | 10000 req/min |

---

## 7. API Documentation

### OpenAPI/Swagger

- Document all endpoints
- Include request/response examples
- Document error codes
- Add tags for grouping

### Example

```yaml
/api/v1/payments:
  post:
    summary: Create a new payment
    tags:
      - Payments
    security:
      - BearerAuth: []
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/PaymentRequest'
          example:
            amount: 100.00
            currency: USD
    responses:
      201:
        description: Payment created
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PaymentResponse'
```

---

## Quick Reference

| Aspect | Standard |
|--------|----------|
| URL | /api/v1/{resource} |
| Versioning | URL path |
| Pagination | page, size |
| Errors | Consistent format |
| Rate Limit | Headers |
| GraphQL | DataLoader for N+1 |