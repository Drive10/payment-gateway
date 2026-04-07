# Search Service - Agent Rules

> Specific guidelines for AI agents working on the search-service

---

## Service Overview

- **Port**: 8088
- **Database**: Elasticsearch
- **Dependencies**: Kafka

---

## Key Responsibilities

1. Full-text search
2. Payment and order search
3. Aggregations and analytics

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/search/payments | Search payments |
| GET | /api/v1/search/orders | Search orders |

---

## Elasticsearch Indices

| Index | Purpose |
|-------|---------|
| payments | Payment search |
| orders | Order search |
| merchants | Merchant search |

---

## Related Docs

- [.ai/rules/database.md](../../.ai/rules/database.md)
- [.ai/rules/devops.md](../../.ai/rules/devops.md)