# PayFlow Operations Runbook

> Production operational procedures for PayFlow payment platform

---

## 🚨 Incident Response

### Severity Levels

| Level | Response Time | Examples |
|-------|---------------|----------|
| **P0 - Critical** | 15 min | Payment processing down, data breach |
| **P1 - High** | 1 hour | Webhook failures, provider outage |
| **P2 - Medium** | 4 hours | Elevated latency, partial failures |
| **P3 - Low** | 24 hours | Non-critical bugs, optimization |

### Critical Incident Checklist

- [ ] Acknowledge alert in PagerDuty/Slack
- [ ] Join incident channel: `#incident-payflow`
- [ ] Identify affected services
- [ ] Check dashboards for error rates
- [ ] Review recent deployments
- [ ] Escalate to on-call if needed

---

## 🔍 Common Issues

### 1. High Payment Failure Rate

**Symptoms**: `payment.failed.total` metric spikes

**Diagnosis**:
```bash
# Check provider health
curl http://localhost:9090/api/v1/query?query=payment_failed_total

# Check circuit breaker state
curl http://localhost:8083/actuator/health | jq '.components.resilience4j'

# Review recent logs
kubectl logs -f -l app=payment-service --tail=100 | grep FAILED
```

**Resolution**:
1. Check provider status (Stripe/Razorpay/PayPal dashboards)
2. If provider down → circuit breaker open → manual intervention needed
3. Review failure reasons in logs
4. If rate limit hit → wait for reset

### 2. Webhook Processing Failures

**Symptoms**: `webhook.received.total` ≠ `webhook.processed.total`

**Diagnosis**:
```bash
# Check webhook delivery status
curl http://localhost:8083/api/v1/admin/webhooks/stats

# Review failed webhooks
curl http://localhost:8083/api/v1/admin/webhooks/failed
```

**Resolution**:
1. Verify webhook secret is correct
2. Check signature validation logs
3. Retry failed webhooks via admin endpoint
4. If provider signature changed → update secret

### 3. Kafka Consumer Lag

**Symptoms**: High consumer lag in Prometheus

**Diagnosis**:
```bash
# Check consumer groups
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --list

# Check lag
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --group payment-service --describe
```

**Resolution**:
1. Scale consumer instances
2. Check for poison messages in DLQ
3. Review consumer processing time
4. Restart stuck consumer

### 4. Database Connection Exhaustion

**Symptoms**: HikariCP pool exhausted errors

**Diagnosis**:
```bash
# Check active connections
psql -h postgres -U payflow -c "SELECT count(*) FROM pg_stat_activity WHERE datname='payflow_payment';"

# Check Hikari metrics
curl http://localhost:8083/actuator/metrics/hikaricp.connections.active
```

**Resolution**:
1. Increase `maximum-pool-size` in config
2. Check for long-running transactions
3. Kill stuck queries
4. Restart service if needed

---

## 🛠️ Operational Commands

### Manual Payment Retry

```bash
# Via API
curl -X POST http://localhost:8083/api/v1/payments/{paymentId}/retry \
  -H "Authorization: Bearer {admin-token}" \
  -H "Idempotency-Key: manual-retry-{timestamp}"
```

### Force Webhook Reprocess

```bash
# Via API
curl -X POST http://localhost:8083/api/v1/admin/webhooks/{eventId}/retry \
  -H "Authorization: Bearer {admin-token}"
```

### Drain DLQ Messages

```bash
# List DLQ topics
kafka-topics.sh --bootstrap-server kafka:9092 --list | grep .dlt

# Replay from beginning
kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic payment.events.dlt --from-beginning | jq .
```

### Update Provider Configuration

```bash
# Update via configmap (Kubernetes)
kubectl patch configmap payflow-config -n payflow \
  --patch '{"data":{"RAZORPAY_WEBHOOK_SECRET":"new-secret"}}'

# Restart service
kubectl rollout restart deployment/payment-service -n payflow
```

---

## 📊 Health Checks

### Service Health Matrix

| Service | Endpoint | Expected |
|---------|----------|----------|
| payment-service | `GET /actuator/health` | `UP` |
| payment-service | `GET /actuator/health/db` | `UP` |
| payment-service | `GET /actuator/health/redis` | `UP` |
| payment-service | `GET /actuator/health/kafka` | `UP` |

### Deep Health Check Script

```bash
#!/bin/bash
SERVICES=("8081" "8082" "8083" "8084")
for port in "${SERVICES[@]}"; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
  if [ "$status" != "200" ]; then
    echo "FAIL: Service on port $port returned $status"
  else
    echo "OK: Service on port $port"
  fi
done
```

---

## 🔄 Deployment Procedures

### Blue-Green Deployment

1. Deploy new version to blue environment
2. Run smoke tests
3. Switch load balancer
4. Monitor error rates
5. If healthy → complete; if issues → rollback

### Rollback Procedure

```bash
# Kubernetes rollback
kubectl rollout undo deployment/payment-service -n payflow

# Verify rollback
kubectl rollout status deployment/payment-service -n payflow
```

---

## 📈 Key Dashboards

### Grafana Dashboards

| Dashboard | Purpose |
|-----------|---------|
| Payment Overview | Success/failure rates, volumes |
| Provider Health | Per-provider metrics |
| Webhook Performance | Processing time, failure rates |
| Service Latency | P50, P95, P99 by endpoint |
| Database Performance | Connection pool, query times |

---

## 📞 Escalation Contacts

| Role | Contact |
|------|---------|
| On-Call Engineer | PagerDuty rotation |
| Engineering Lead | @engineering-lead |
| Security | security@payflow.dev |

---

## 📋 Post-Incident Checklist

- [ ] Write incident summary
- [ ] Identify root cause
- [ ] Document action items
- [ ] Schedule retrospective
- [ ] Update runbook if needed
- [ ] Close incident ticket
