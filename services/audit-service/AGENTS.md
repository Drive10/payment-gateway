# Audit Service - Agent Rules

> Specific guidelines for AI agents working on the audit-service

---

## Service Overview

- **Port**: 8089
- **Database**: MongoDB

---

## Key Responsibilities

1. MongoDB audit logging
2. Event sourcing
3. Compliance tracking
4. User activity logs

---

## Collection Schema

```json
{
  "eventType": "PAYMENT_CREATED",
  "entityType": "Payment",
  "entityId": "uuid",
  "userId": "uuid",
  "timestamp": "ISO-8601",
  "changes": { }
}
```

---

## Related Docs

- [.ai/rules/database.md](../../.ai/rules/database.md)