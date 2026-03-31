# Incident Response Playbook

## Table of Contents
- [Incident Severity Levels](#incident-severity-levels)
- [Response Timeline](#response-timeline)
- [Service-Specific Procedures](#service-specific-procedures)
- [Communication Templates](#communication-templates)
- [Post-Incident Process](#post-incident-process)

---

## Incident Severity Levels

| Severity | Definition | Response Time | Examples |
|----------|------------|---------------|----------|
| **P1 - Critical** | Complete service outage or data breach | Immediate (15 min) | Payment gateway down, fraud detected, data exfiltration |
| **P2 - High** | Major feature degraded, significant customer impact | 30 minutes | Checkout failures >5%, payment processing delays |
| **P3 - Medium** | Partial degradation, limited impact | 2 hours | Notification delays, dashboard slow |
| **P4 - Low** | Minor issue, workaround available | 24 hours | Logging errors, minor UI bugs |

---

## Response Timeline

### P1 Critical Incident

```
T+0:00    - Alert triggered / Incident detected
T+0:15    - Incident Commander (IC) acknowledges
T+0:30    - War room activated, initial assessment complete
T+1:00    - Status page updated, stakeholders notified
T+2:00    - Root cause identified or mitigation deployed
T+4:00    - Service restored (target)
T+24:00   - Post-incident report initiated
T+72:00   - Post-incident review completed
```

---

## Service-Specific Procedures

### Payment Service (Port 8080)

**Critical Dependencies:**
- PostgreSQL (payments database)
- Kafka (event streaming)
- Redis (session/cache)
- Risk Service (fraud checks)

**Failure Modes:**

#### Payment Gateway Unavailable
1. Check database connectivity: `kubectl exec -it payment-service-xxx -- nc -zv postgres 5432`
2. Verify Kafka producer health: Check `/actuator/health` for kafka binder status
3. Review recent deployments: `kubectl rollout history deployment/payment-service`
4. **Rollback if needed:** `kubectl rollout undo deployment/payment-service`

#### High Transaction Latency
1. Check connection pool metrics in Grafana
2. Identify slow queries: Query `pg_stat_statements`
3. Scale horizontally: `kubectl scale deployment payment-service --replicas=5`

**Circuit Breaker Actions:**
```bash
# View circuit breaker state
curl http://payment-service:8080/actuator/circuitbreakers

# Manually open circuit (if needed)
curl -X POST http://payment-service:8080/actuator/circuitbreakers/{name}/open
```

---

### API Gateway (Port 8081)

**Critical Dependencies:**
- Auth Service
- Payment Service
- Rate limiting Redis

**Failure Modes:**

#### Gateway Timeout Errors
1. Check upstream service health
2. Verify rate limiter Redis connectivity
3. Review request timeout configurations

**Commands:**
```bash
# Check gateway logs
kubectl logs -f -l app=api-gateway

# View routing rules
curl http://api-gateway:8081/actuator/gateway/routes
```

---

### Risk Service (Port 8088)

**Critical Dependencies:**
- PostgreSQL (risk database)
- ML Model artifacts (S3)

**Failure Modes:**

#### Risk Assessment Failures
1. Check ML model availability: `curl http://risk-service:8088/actuator/health`
2. Verify database connectivity
3. Fallback to rule-based scoring (built-in)

**Commands:**
```bash
# Force fallback mode
curl -X PUT http://risk-service:8088/api/risk/config -d '{"mode":"RULE_ONLY"}'

# View risk rules
curl http://risk-service:8088/api/risk/rules
```

---

### Notification Service (Port 8085)

**Critical Dependencies:**
- PostgreSQL (templates/notifications)
- SMTP/SMS providers (external)
- Webhook endpoints

**Failure Modes:**

#### Email Delivery Failures
1. Check SMTP provider status (SendGrid/AWS SES dashboard)
2. Verify template rendering: Check application logs
3. Review delivery queue: Kafka consumer lag

**Commands:**
```bash
# View notification queue status
curl http://notification-service:8085/api/notifications/queue/stats

# Retry failed notifications
curl -X POST http://notification-service:8085/api/notifications/retry -d '{"status":"FAILED"}'

# Manual webhook test
curl -X POST http://notification-service:8085/api/notifications/webhook/test
```

---

### Auth Service (Port 8083)

**Critical Dependencies:**
- PostgreSQL (users/sessions)
- Redis (token blacklist)

**Failure Modes:**

#### Authentication Failures
1. Check Redis connectivity for session management
2. Verify JWT signing keys are available
3. Check token expiration configuration

**Commands:**
```bash
# Invalidate all sessions for user
curl -X DELETE http://auth-service:8083/api/auth/sessions/{userId}

# Force re-authentication
curl -X POST http://auth-service:8083/api/auth/force-reauth -d '{"userId":"xxx"}'
```

---

### Webhook Service (Port 8090)

**Critical Dependencies:**
- PostgreSQL (delivery attempts)
- External merchant endpoints

**Failure Modes:**

#### Webhook Delivery Failures
1. Check merchant endpoint health
2. Review retry queue: Kafka dead letter topic
3. Manual retry via admin API

**Commands:**
```bash
# View webhook delivery stats
curl http://webhook-service:8090/api/webhooks/stats

# Manual retry
curl -X POST http://webhook-service:8090/api/webhooks/{id}/retry

# Purge stuck webhooks
curl -X DELETE http://webhook-service:8090/api/webhooks/stuck
```

---

## Communication Templates

### Status Page Update - Active Incident

```
[INVESTIGATING] We are investigating reports of payment processing delays.
Our team is actively working to resolve the issue. We will provide an update
in 30 minutes.

Impact: Checkout failures for some customers
Start Time: YYYY-MM-DD HH:MM UTC
Next Update: YYYY-MM-DD HH:MM UTC
```

### Customer Email - Critical Issue

```
Subject: [URGENT] Service Disruption Notice - [Service Name]

Dear [Customer Name],

We are experiencing a temporary service disruption affecting [service/feature].
Our engineering team is actively working to resolve this issue.

What you need to know:
- Issue started: [timestamp]
- Current impact: [description]
- Expected resolution: [timeframe]

We apologize for any inconvenience. No customer data has been compromised.

Thank you for your patience,
[Company] Engineering Team
```

### Stakeholder Update

```
INCIDENT UPDATE - [PRIORITY] - [Service Name]

Status: [INVESTIGATING/IDENTIFIED/MONITORING/RESOLVED]
Time: [Current Time] UTC
Duration: [X hours Y minutes]

Current Status:
[Brief description of situation]

Impact:
- [Impact area 1]
- [Impact area 2]

Actions Taken:
1. [Action 1]
2. [Action 2]

Next Steps:
- [Next action 1]
- [Next action 2]

ETA to Resolution: [Time]
```

---

## Post-Incident Process

### 1. Immediate (Within 24 hours)
- Document timeline of events
- Capture all relevant logs and metrics
- Identify all impacted customers

### 2. Root Cause Analysis (Within 72 hours)
- Conduct blameless post-mortem
- Identify contributing factors
- Determine root cause

### 3. Action Items
- Create JIRA tickets for all improvements
- Prioritize by impact/frequency
- Assign owners and deadlines

### 4. Post-Incident Review Meeting
- Review timeline
- Discuss what went well
- Discuss what could improve
- Finalize action items

### Post-Incident Report Template

```markdown
# Post-Incident Report: [Incident Title]

**Date:** YYYY-MM-DD
**Severity:** P[1-4]
**Duration:** X hours Y minutes
**Status:** [Draft/Final]

## Summary
[Brief description of incident]

## Impact
- [Customer impact]
- [Revenue impact]
- [Reputational impact]

## Timeline
| Time | Event |
|------|-------|
| HH:MM | Alert triggered |
| HH:MM | IC acknowledged |
| ... | ... |

## Root Cause
[Technical explanation]

## Contributing Factors
1. [Factor 1]
2. [Factor 2]

## Resolution
[How issue was fixed]

## Action Items
| Item | Owner | Priority | Due Date |
|------|-------|----------|----------|
| [Desc] | [Name] | High | YYYY-MM-DD |
```

---

## Escalation Contacts

| Role | Name | Contact | Hours |
|------|------|---------|-------|
| On-Call Engineer | Rotating | PagerDuty | 24/7 |
| Engineering Manager | [Name] | [Phone] | Business hours |
| VP Engineering | [Name] | [Phone] | P1 only |
| Security Team | [Name] | [Email] | 24/7 |
| Customer Success | [Name] | [Email] | Business hours |

---

## Useful Commands

### Kubernetes Operations
```bash
# View pod status
kubectl get pods -n payment-gateway

# View pod logs
kubectl logs -f deployment/payment-service -n payment-gateway

# Describe pod for events
kubectl describe pod <pod-name> -n payment-gateway

# Scale service
kubectl scale deployment payment-service --replicas=3 -n payment-gateway

# Rollback deployment
kubectl rollout undo deployment/payment-service -n payment-gateway

# Execute into pod
kubectl exec -it <pod-name> -n payment-gateway -- /bin/sh
```

### Database Operations
```bash
# Connect to PostgreSQL
kubectl exec -it postgres-0 -n payment-gateway -- psql -U postgres -d paymentdb

# View active connections
SELECT * FROM pg_stat_activity WHERE datname = 'paymentdb';

# Kill long-running query
SELECT pg_cancel_backend(pid);
```

### Service Health Checks
```bash
# Check all service health
for svc in api-gateway payment-service auth-service notification-service risk-service; do
  echo "$svc: $(curl -s http://$svc:8080/actuator/health | jq -r '.status')"
done
```

---

## Appendix: Runbook Links

- [Database Failover Procedure](./runbooks/database-failover.md)
- [Kafka Consumer Lag Resolution](./runbooks/kafka-lag.md)
- [Redis Cache Recovery](./runbooks/redis-recovery.md)
- [SSL Certificate Renewal](./runbooks/ssl-renewal.md)
