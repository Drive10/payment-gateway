# Simulator Service - Agent Rules

> Specific guidelines for AI agents working on the simulator-service

---

## Service Overview

- **Port**: 8086
- **Dependencies**: Kafka

---

## Key Responsibilities

1. Payment simulation for testing
2. Load testing support
3. Mock payment providers

---

## Usage

```bash
# Test payment with test card
curl -X POST http://localhost:8086/api/v1/simulate/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 100, "currency": "USD", "testMode": "SUCCESS"}'

# Test card numbers
# Success: 4111111111111111
# Decline: 4000000000000002
# Insufficient funds: 4000000000009995
```

---

## Related Docs

- [.ai/rules/backend.md](../../.ai/rules/backend.md)
- [.ai/rules/payment.md](../../.ai/rules/payment.md)