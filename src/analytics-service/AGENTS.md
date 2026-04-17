# Analytics Service - Agent Rules

> Specific guidelines for AI agents working on the analytics-service

---

## Service Overview

- **Port**: 8089
- **Database**: PostgreSQL, Elasticsearch
- **Dependencies**: Kafka, Payment Service

---

## Key Responsibilities

1. Revenue analytics
2. Payment trends
3. Merchant reports
4. Risk scoring
5. Settlements and disputes

---

## Important Files

```
services/analytics-service/
├── src/main/java/com/payflow/analytics/
│   ├── controller/AnalyticsController.java
│   ├── service/AnalyticsService.java
│   └── service/RiskScoringService.java
└── src/main/resources/application.yml
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/analytics/summary | Revenue summary |
| GET | /api/v1/analytics/reports | Generate reports |
| GET | /api/v1/analytics/trends | Payment trends |
| GET | /api/v1/analytics/merchant/{id} | Merchant analytics |

---

## Related Docs

- [.ai/rules/backend.md](../../.ai/rules/backend.md)
- [.ai/rules/database.md](../../.ai/rules/database.md)