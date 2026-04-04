# Kafka Schema Registry - Avro Schemas for Payment Gateway Events
# Schema evolution: BACKWARD compatibility (new fields must be optional)

## Payment Events

### payment-created.avsc
```json
{
  "type": "record",
  "name": "PaymentCreated",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "paymentId", "type": "string"},
    {"name": "orderId", "type": "string"},
    {"name": "merchantId", "type": "string"},
    {"name": "amount", "type": "long"},
    {"name": "currency", "type": "string"},
    {"name": "provider", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "createdAt", "type": "long"},
    {"name": "metadata", "type": ["null", "string"], "default": null}
  ]
}
```

### payment-status-updated.avsc
```json
{
  "type": "record",
  "name": "PaymentStatusUpdated",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "paymentId", "type": "string"},
    {"name": "previousStatus", "type": "string"},
    {"name": "newStatus", "type": "string"},
    {"name": "updatedAt", "type": "long"},
    {"name": "providerReference", "type": ["null", "string"], "default": null}
  ]
}
```

### payment-failed.avsc
```json
{
  "type": "record",
  "name": "PaymentFailed",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "paymentId", "type": "string"},
    {"name": "errorCode", "type": "string"},
    {"name": "errorMessage", "type": "string"},
    {"name": "failedAt", "type": "long"},
    {"name": "retryable", "type": "boolean", "default": false}
  ]
}
```

## Order Events

### order-created.avsc
```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "merchantId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "long"},
    {"name": "currency", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "createdAt", "type": "long"}
  ]
}
```

### order-status-updated.avsc
```json
{
  "type": "record",
  "name": "OrderStatusUpdated",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "previousStatus", "type": "string"},
    {"name": "newStatus", "type": "string"},
    {"name": "updatedAt", "type": "long"}
  ]
}
```

## Webhook Events

### webhook-delivery-attempt.avsc
```json
{
  "type": "record",
  "name": "WebhookDeliveryAttempt",
  "namespace": "dev.payment.events",
  "fields": [
    {"name": "webhookId", "type": "string"},
    {"name": "eventType", "type": "string"},
    {"name": "attemptNumber", "type": "int"},
    {"name": "url", "type": "string"},
    {"name": "responseCode", "type": ["null", "int"], "default": null},
    {"name": "deliveredAt", "type": "long"}
  ]
}
```

## Schema Evolution Rules

1. **BACKWARD compatibility** - New consumers can read old data
2. **Adding fields** - Must have a default value
3. **Removing fields** - Must be optional (nullable)
4. **Renaming fields** - Not allowed, add new field + deprecate old
5. **Changing types** - Only to compatible types (int -> long)

## Topic Configuration

| Topic | Partitions | Replication | Retention |
|-------|------------|-------------|-----------|
| payment.events | 6 | 3 | 7 days |
| order.events | 6 | 3 | 7 days |
| webhook.updates | 3 | 3 | 3 days |
| notification.events | 3 | 3 | 1 day |
| analytics.events | 3 | 3 | 30 days |
| payment.status | 6 | 3 | 7 days |
