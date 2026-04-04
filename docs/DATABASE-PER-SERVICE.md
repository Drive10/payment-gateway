# Database Per Service Architecture

## Current State: Shared PostgreSQL
All services share a single PostgreSQL instance with separate databases.

## Target State: Database Per Service
Each service has its own dedicated database with isolated credentials.

## Service Database Mapping

| Service | Database | User | Port |
|---------|----------|------|------|
| auth-service | authdb | auth | 5433 |
| order-service | orderdb | ord | 5433 |
| payment-service | paymentdb | payment | 5433 |
| notification-service | notificationdb | notification | 5433 |
| analytics-service | analyticsdb | analytics | 5433 |
| simulator-service | simulatordb | simulator | 5433 |

## Migration Strategy

### Phase 1: Logical Separation (Current)
- Separate databases per service
- Separate users with limited privileges
- Shared PostgreSQL instance

### Phase 2: Physical Separation (Future)
- Separate PostgreSQL instances per service
- Independent scaling and backup
- Service-specific connection pooling

### Phase 3: Polyglot Persistence (Future)
- PostgreSQL for transactional data
- Redis for caching and sessions
- Elasticsearch for search and analytics
- TimescaleDB for time-series data

## Connection Configuration

Each service configures its own datasource:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Security Controls

- Each service user has access only to its own database
- No cross-service database queries
- Connection pooling with HikariCP
- SSL/TLS for database connections (production)
- Regular password rotation

## Backup Strategy

- Daily automated backups
- Point-in-time recovery (PITR)
- Cross-region replication (production)
- Backup encryption at rest
- Regular restore testing
