# Chaos Engineering - Fault Injection Tests
# Tests system resilience by injecting failures in production-like environments

## Test Scenarios

### 1. Database Connection Failure
```bash
# Simulate PostgreSQL going down
kubectl exec -it payment-gateway-postgres-1 -- pg_ctlcluster 16 main stop

# Expected behavior:
# - Services retry with exponential backoff
# - Circuit breaker opens after 5 failures
# - Health checks report degraded state
# - Services recover when DB comes back

# Verify recovery
kubectl exec -it payment-gateway-postgres-1 -- pg_ctlcluster 16 main start
```

### 2. Kafka Broker Failure
```bash
# Kill one Kafka broker
docker stop payment-gateway-kafka-1

# Expected behavior:
# - Producers retry with backoff
# - Consumers continue from other brokers
# - No message loss (replication factor 3)

# Verify recovery
docker start payment-gateway-kafka-1
```

### 3. Redis Cache Failure
```bash
# Stop Redis
docker stop payment-gateway-redis-1

# Expected behavior:
# - Rate limiting falls back to local in-memory
# - Sessions continue with degraded performance
# - No service outage

# Verify recovery
docker start payment-gateway-redis-1
```

### 4. Network Partition
```bash
# Simulate network partition between services
docker network disconnect dev-network payment-gateway-api-gateway-1

# Expected behavior:
# - Gateway returns 503 for affected services
# - Other services continue operating
# - Health checks detect partition

# Verify recovery
docker network connect dev-network payment-gateway-api-gateway-1
```

### 5. High Load Test
```bash
# Generate 10x normal load
k6 run --vus 1000 --duration 5m tests/load/scenarios/full-flow.js

# Expected behavior:
# - Auto-scaling triggers
# - Rate limiting prevents overload
# - No cascading failures
# - Error rate stays < 1%
```

### 6. Memory Pressure
```bash
# Force GC and memory pressure
kubectl exec -it payment-gateway-payment-service-1 -- \
  jcmd 1 GC.run

# Expected behavior:
# - Services handle GC pauses gracefully
# - No OOM kills
# - Health checks remain responsive
```

### 7. Disk Full
```bash
# Fill disk to 95%
docker exec payment-gateway-postgres-1 \
  dd if=/dev/zero of=/tmp/fill bs=1M count=1024

# Expected behavior:
# - Services detect low disk space
# - Alert triggered
# - Services continue operating

# Cleanup
docker exec payment-gateway-postgres-1 rm /tmp/fill
```

## Chaos Testing Schedule

| Test | Frequency | Environment |
|------|-----------|-------------|
| DB Connection Failure | Weekly | Staging |
| Kafka Broker Failure | Monthly | Staging |
| Redis Cache Failure | Weekly | Staging |
| Network Partition | Monthly | Staging |
| High Load Test | Daily | Staging |
| Memory Pressure | Weekly | Staging |
| Disk Full | Monthly | Staging |

## Success Criteria

- **Recovery Time**: < 30 seconds for all scenarios
- **Data Loss**: Zero message loss in all scenarios
- **User Impact**: < 1% error rate during failures
- **Alert Time**: < 1 minute to detect and alert
