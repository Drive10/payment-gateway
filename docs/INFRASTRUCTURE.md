# PayFlow Infrastructure

> Infrastructure setup, services, and configuration

---

## Docker Services

### Infrastructure Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| **postgres** | postgres:16-alpine | 5432 | Primary database |
| **redis** | redis:7-alpine | 6379 | Cache, rate limiting |
| **zookeeper** | confluentinc/cp-zookeeper:7.5.0 | 2181 | Kafka coordination |
| **kafka** | confluentinc/cp-kafka:7.5.0 | 9092 | Event streaming |
| **prometheus** | prom/prometheus:v2.50.1 | 9090 | Metrics collection |
| **loki** | grafana/loki:2.9.5 | 3100 | Log aggregation |
| **grafana** | grafana/grafana:11.0.0 | 3000 | Dashboards |

### Application Services

| Service | Port | Health Check |
|---------|------|--------------|
| **api-gateway** | 8080 | /actuator/health |
| **auth-service** | 8081 | /actuator/health |
| **order-service** | 8082 | /actuator/health |
| **payment-service** | 8083 | /actuator/health |
| **notification-service** | 8084 | /actuator/health |
| **simulator-service** | 8086 | /actuator/health |
| **frontend** | 5173 | N/A |

---

## Environment Variables

### Database
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | payflow | PostgreSQL username |
| `DB_PASSWORD` | payflow | PostgreSQL password |
| `DB_NAME` | payflow | Database name |

### Redis
| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_PASSWORD` | devpassword | Redis password |

### Kafka
| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | kafka:29092 | Kafka brokers |

### Security
| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | JWT signing key (base64) |
| `GATEWAY_INTERNAL_SECRET` | Internal gateway secret |

---

## Network

- Network name: `payflow-network`
- Driver: `bridge`

### Service Communication
```bash
# From services, access services via:
postgres:5432
redis:6379
kafka:29092
```

---

## Volumes

| Volume | Service | Path |
|--------|---------|------|
| `postgres_data` | postgres | /var/lib/postgresql/data |
| `prometheus_data` | prometheus | /prometheus |
| `loki_data` | loki | /loki |
| `grafana_data` | grafana | /var/lib/grafana |

---

## Health Checks

All services use wget to check `/actuator/health`:
```yaml
test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
interval: 30s
timeout: 10s
retries: 3
start_period: 30s
```

---

## Monitoring Stack

Monitoring is included in `docker-compose.yml` under `infra`/`services` profiles:
- Prometheus (9090)
- Grafana (3000)
- Loki (3100)
- Zipkin (9411)

---

## Commands

```bash
# Start all services
docker compose up -d

# Start infrastructure only
docker compose --profile infra up -d

# Start monitoring + infra only
docker compose --profile infra up -d

# View logs
docker compose logs -f

# Stop all
docker compose down

# Rebuild images
docker compose build --no-cache
```
