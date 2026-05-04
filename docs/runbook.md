# PayFlow On-Call Runbook

## Emergency Contacts
- On-call: +1-555-PAYFLOW (729-3569)
- Engineering Lead: @engineering-lead
- Platform Team: #platform-oncall
- Incident Channel: #incidents

---

## SLOs
| Metric | SLO | Alert |
|--------|-----|-------|
| Payment Latency (p95) | < 500ms | 5min |
| Payment Success Rate | > 99% | 5min |
| Webhook Lag | < 5sec | 2min |
| Reconciliation Mismatch | 0 | 1min |

---

## Incident: High Payment Latency

### Symptoms
- `PaymentLatencyHigh` alert firing
- Users reporting slow payment processing

### Diagnosis
```bash
# Check service metrics
curl -s localhost:8083/actuator/metrics | jq

# Check database query times
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT query, calls, mean_time, total_time 
  FROM pg_stat_statements 
  ORDER BY mean_time DESC 
  LIMIT 10;
"

# Check connection pool
curl -s localhost:8083/actuator/metrics/hikaricp | jq
```

### Mitigation
1. **If database queries slow**: Kill long-running queries
   ```bash
   docker exec payflow-postgres psql -U payflow -d payflow -c "
     SELECT pg_terminate_backend(pid) FROM pg_stat_activity 
     WHERE state = 'active' AND query LIKE '%payments%';
   "
   ```

2. **If connection pool full**: Increase pool size in config
   ```yaml
   spring.datasource.hikari.maximum-pool-size: 20
   ```

3. **If CPU bound**: Scale out payment service
   ```bash
   docker compose up -d --scale payment-service=3
   ```

### Rollback
```bash
docker compose up -d --scale payment-service=1
```

---

## Incident: Payment Failures

### Symptoms
- `PaymentSuccessRateLow` alert firing
- Users unable to complete payments

### Diagnosis
```bash
# Check payment service logs
docker logs payflow-payment-service --since 5m | grep ERROR

# Check for database issues
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT status, COUNT(*) FROM payments 
  WHERE created_at > NOW() - INTERVAL '5 minutes' 
  GROUP BY status;
"

# Check provider status
curl -s http://simulator-service:8086/health
```

### Mitigation
1. **Check provider**: If simulator down, restart
   ```bash
   docker compose restart simulator-service
   ```

2. **Check database locked rows**
   ```bash
   docker exec payflow-postgres psql -U payflow -d payflow -c "
     SELECT * FROM payments WHERE status = 'PROCESSING' 
     AND created_at < NOW() - INTERVAL '10 minutes';
   "
   ```

3. **Retry stuck payments**
   ```bash
   curl -X POST localhost:8083/admin/payments/retry-stuck
   ```

---

## Incident: Webhook Not Processing

### Symptoms
- `WebhookProcessingLag` alert firing
- `webhooks_received` != `webhooks_processed`

### Diagnosis
```bash
# Check webhook inbox events
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT status, COUNT(*) FROM webhook_inbox_events 
  GROUP BY status;
"

# Check Kafka consumer lag
docker exec payflow-kafka kafka-consumer-groups.sh --bootstrap-server kafka:9092 --list
```

### Mitigation
1. **Replay from inbox**
   ```bash
   docker exec payflow-payment-service java -jar app.jar replay-webhooks
   ```

2. **Restart webhook consumer**
   ```bash
   docker compose restart payment-service
   ```

---

## Incident: DLQ Growing

### Symptoms
- `DeadLetterQueueGrowing` alert firing

### Diagnosis
```bash
# Check dlq events
curl -s localhost:8083/admin/events/dlq | jq

# Check logs for pattern
docker logs payflow-payment-service --since 10m | grep "dead-letter"
```

### Mitigation
1. **Analyze errors**
   ```bash
   curl -s localhost:8083/admin/events/dlq | jq '.[].errorMessage' | sort | uniq -c
   ```

2. **Replay specific event**
   ```bash
   curl -X POST localhost:8083/admin/events/replay/{eventId}
   ```

3. **Replay all**
   ```bash
   curl -X POST localhost:8083/admin/events/replay/all
   ```

---

## Incident: Reconciliation Mismatch

### Symptoms
- `ReconciliationMismatches` alert firing

### Diagnosis
```bash
# Check mismatches
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT p.id, p.status as local, provider.status as remote
  FROM payments p
  JOIN provider_transactions pt ON p.provider_reference = pt.reference
  WHERE p.status != pt.status
  AND p.updated_at > NOW() - INTERVAL '1 hour';
"
```

### Mitigation
1. **Run manual reconciliation**
   ```bash
   curl -X POST localhost:8083/admin/reconciliation/run
   ```

2. **Check provider API keys**
   ```yaml
   app.merchant.api-keys: ${MERCHANT_API_KEYS}
   ```

---

## Incident: Settlement Failure

### Symptoms
- `SettlementFailuresHigh` alert

### Diagnosis
```bash
# Check settlement batches
curl -s localhost:8083/admin/finance/settlements?status=FAILED | jq

# Check bank/API status
curl -s http://notification-service:8085/health
```

### Mitigation
1. **Retry failed batch**
   ```bash
   curl -X POST localhost:8083/admin/finance/settlements/{id}/retry
   ```

2. **Manual payout**
   ```bash
   curl -X POST localhost:8083/admin/finance/payout-manual \
     -d '{"batchId": "xxx", "method": "BANK_TRANSFER"}'
   ```

---

## Dependency Degradation

### Payment Service -> Auth Service
- Auth down → Allow guest checkout (disable JWT verification)
- `app.auth-service.url: http://auth-service:8082` → `fallback: true`

### Payment Service -> Simulator
- Simulator down → Use cached responses
- `app.simulator.url: http://simulator-service:8086` → `fallback: true`

### Payment Service -> Database
- DB read-only → Queue writes
- Enable read-replica: `spring.datasource.read-url: ...`

---

## Rollback Procedures

### Quick Rollback (last 10min)
```bash
# Uses DB transaction log
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT pg_undo txn_id FROM pglogical.replication 
  WHERE origin_name = 'payment_service'
  ORDER BY txn_timestamp DESC LIMIT 1;
"
```

### Full Database Rollback
```bash
# WARNING: DESTRUCTIVE - Use only for P0
docker exec payflow-postgres psql -U payflow -d payflow -c "
  SELECT pg_revert_logical_replication('payment_service');
"
```

---

## Recovery Steps

1. **Acknowledge alert** in PagerDuty
2. **Create incident** in #incidents channel
3. **Follow runbook** for specific incident type
4. **Update status**: 
   - `Investigating` → `Identified` → `Mitigating` → `Resolved`
5. **Post-mortem** within 48 hours