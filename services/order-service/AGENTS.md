# Order Service - Agent Rules

> Specific guidelines for AI agents working on the order-service

---

## Service Overview

- **Port**: 8082
- **Database**: PostgreSQL
- **Dependencies**: Payment Service, Auth Service

---

## Key Responsibilities

1. Order creation and management
2. Merchant management
3. API key generation
4. KYC (Know Your Customer)

---

## Important Files

```
services/order-service/
├── src/main/java/com/payflow/order/
│   ├── controller/OrderController.java
│   ├── service/OrderService.java
│   ├── repository/OrderRepository.java
│   └── dto/OrderRequest.java, OrderResponse.java
└── src/main/resources/application.yml
```

---

## Critical Rules

1. **Always validate merchant** - Check API key validity
2. **Use UUID for order IDs** - Never use auto-increment
3. **Implement idempotency** - For order creation

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/orders | Create order |
| GET | /api/v1/orders | List orders |
| GET | /api/v1/orders/{id} | Get order |
| PUT | /api/v1/orders/{id} | Update order |
| DELETE | /api/v1/orders/{id} | Cancel order |

---

## Related Docs

- [.ai/rules/backend.md](../../.ai/rules/backend.md)
- [.ai/rules/api.md](../../.ai/rules/api.md)
- [.ai/context/services.md](../../.ai/context/services.md)