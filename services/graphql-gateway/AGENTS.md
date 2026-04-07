# GraphQL Gateway - Agent Rules

> Specific guidelines for AI agents working on the graphql-gateway

---

## Service Overview

- **Port**: 8087
- **Dependencies**: All services (federation)

---

## Key Responsibilities

1. GraphQL API with schema federation
2. Real-time subscriptions (WebSocket)
3. DataLoader for N+1 prevention

---

## Endpoints

| Endpoint | Description |
|----------|-------------|
| /graphql | GraphQL endpoint |
| /graphiql | Interactive playground |

---

## Related Docs

- [.ai/rules/api.md](../../.ai/rules/api.md)
- [.ai/rules/backend.md](../../.ai/rules/backend.md)