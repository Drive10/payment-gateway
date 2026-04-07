# Notification Service - Agent Rules

> Specific guidelines for AI agents working on the notification-service

---

## Service Overview

- **Port**: 8084
- **Dependencies**: Kafka, Redis (feature flags), External providers

---

## Key Responsibilities

1. Email notifications
2. SMS notifications
3. Push notifications
4. Webhook delivery
5. Feature flags

---

## Important Files

```
services/notification-service/
├── src/main/java/com/payflow/notification/
│   ├── controller/NotificationController.java
│   ├── service/NotificationService.java
│   ├── consumer/PaymentEventConsumer.java
│   └── config/EmailConfig.java
└── src/main/resources/application.yml
```

---

## Critical Rules

1. **Async processing** - Don't block on notifications
2. **Retry webhooks** - Implement exponential backoff
3. **Feature flags** - Use Redis for runtime toggles

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/notifications/send | Send notification |
| GET | /api/v1/notifications/templates | List templates |
| GET | /api/v1/webhooks | List webhooks |
| POST | /api/v1/webhooks | Register webhook |

---

## Kafka Topics

| Topic | Event |
|-------|-------|
| payment.completed | Send receipt email |
| payment.failed | Send failure notification |
| order.created | Send order confirmation |

---

## Related Docs

- [.ai/rules/backend.md](../../.ai/rules/backend.md)
- [.ai/rules/devops.md](../../.ai/rules/devops.md)