# Payment Domain Rules

- Payment is NEVER instantly successful
- Always:
  PENDING → webhook → SUCCESS/FAILED

- Must include:
  - idempotency key
  - retry logic
  - failure handling

- Webhook is source of truth
- API response ≠ final state