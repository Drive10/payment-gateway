# PayFlow Infrastructure

> Infrastructure setup, services, and configuration

---

## Docker Services

### Infrastructure Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| **postgres** | postgres:16-alpine | 5432 | Primary database |
| **mongodb** | mongo:7 | 27017 | Audit logs |
| **redis** | redis:7-alpine | 6379 | Cache, rate limiting |
| **zookeeper** | confluentinc/cp-zookeeper:7.5.0 | 2181 | Kafka coordination |
| **kafka** | confluentinc/cp-kafka:7.5.0 | 9092 | Event streaming |

### Application Services

| Service | Port | Health Check |
|---------|------|--------------|
| **api-gateway** | 8080 | /actuator/health |
| **auth-service** | 8081 | /actuator/health |
| **order-service** | 8082 | /actuator/health |
| **payment-service** | 8083 | /actuator/health |
| **notification-service** | 8084 | /actuator/health |
| **simulator-service** | 8086 | /actuator/health |
| **analytics-service** | 8089 | /actuator/health |
| **audit-service** | 8090 | /actuator/health |
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
mongodb:27017
```

---

## Volumes

| Volume | Service | Path |
|--------|---------|------|
| `postgres_data` | postgres | /var/lib/postgresql/data |
| `mongodb_data` | mongodb | /data/db |

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

See `docker-compose.monitoring.yml`:
- Prometheus (9090)
- Grafana (3000)
- Jaeger (16686)

---

## Commands

```bash
# Start all services
docker compose up -d

# Start infrastructure only
docker compose --profile infra up -d

# Start monitoring
docker compose -f docker-compose.monitoring.yml up -d

# View logs
docker compose logs -f

# Stop all
docker compose down

# Rebuild images
docker compose build --no-cache
```