# Backend Agent

Scope:
services/payment-service/**
services/order-service/**

Responsibilities:
- idempotency
- webhook validation
- retry logic
- state machine

Rules:
- no sync success
- enforce async flow