# Service Level Objectives (SLOs) & Service Level Indicators (SLIs)

This document defines the SLOs and SLIs for the payment-gateway system.

---

## 1. Core Payment Service SLOs

| Metric | Target | Error Budget | Measurement Window |
|--------|--------|--------------|---------------------|
| **Availability** | 99.95% | 4h 22m downtime/year | 30-day rolling |
| **Latency (p95)** | < 500ms | 0.05% of requests | 5-minute windows |
| **Latency (p99)** | < 1s | 0.01% of requests | 5-minute windows |
| **Error Rate** | < 0.1% | 0.1% of requests | 30-day rolling |

### Error Budget Calculation (Payment Service)
- **Total minutes per year**: 525,600
- **Target availability**: 99.95% → Max downtime: 262.8 minutes (4h 22m)
- **Monthly budget**: ~22 minutes

---

## 2. API Gateway SLOs

| Metric | Target | Error Budget | Measurement Window |
|--------|--------|--------------|---------------------|
| **Availability** | 99.99% | 52.6 min downtime/year | 30-day rolling |
| **Latency (p95)** | < 200ms | 0.05% of requests | 5-minute windows |
| **Error Rate** | < 0.05% | 0.05% of requests | 30-day rolling |

### Error Budget Calculation (API Gateway)
- **Target availability**: 99.99% → Max downtime: 52.6 minutes/year
- **Monthly budget**: ~4.4 minutes

---

## 3. Auth Service SLOs

| Metric | Target | Error Budget | Measurement Window |
|--------|--------|--------------|---------------------|
| **Login Success Rate** | 99.9% | 0.1% failed logins | 30-day rolling |
| **Token Validation Latency (p95)** | < 100ms | 0.05% of validations | 5-minute windows |

### Error Budget Calculation (Auth Service)
- **Login failures**: Max 0.1% of total login attempts
- **Token validation**: Max 0.05% exceeding 100ms p95

---

## 4. Risk Service SLOs

| Metric | Target | Error Budget | Measurement Window |
|--------|--------|--------------|---------------------|
| **Risk Assessment Latency (p95)** | < 300ms | 0.05% of assessments | 5-minute windows |
| **Availability** | 99.9% | 4h 22m downtime/year | 30-day rolling |

### Error Budget Calculation (Risk Service)
- **Target availability**: 99.9% → Max downtime: 525.6 minutes/year
- **Monthly budget**: ~44 minutes

---

## 5. SLI Definitions

### 5.1 Request Success Rate

```yaml
SLI: Request Success Rate
Measurement: ratio of successful requests to total requests
Formula: sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) 
         / 
         sum(rate(http_server_requests_seconds_count[5m]))
Target: > 99.9% (varies by service)
```

**Classification:**
- **2xx**: Success (authorized, processed)
- **3xx**: Redirect (handled at gateway)
- **4xx**: Client Error (bad request, unauthorized, rate limited)
- **5xx**: Server Error (internal errors, timeouts, unavailable)

### 5.2 Latency Buckets

```yaml
SLI: Request Latency (p50, p95, p99)
Measurement: histogram_quantile over request duration
Formula: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

Buckets:
  - p50 (median): Expected baseline
  - p95: 95th percentile - used for SLO target
  - p99: 99th percentile - used for extreme cases
```

**Latency SLOs by Service:**
| Service | p50 Target | p95 Target | p99 Target |
|---------|------------|------------|------------|
| API Gateway | 50ms | 200ms | 500ms |
| Payment Service | 100ms | 500ms | 1s |
| Auth Service | 20ms | 100ms | 250ms |
| Risk Service | 50ms | 300ms | 500ms |

### 5.3 Error Classification

```yaml
SLI: Error Rate by Category

Client Errors (4xx):
  - 400: Bad Request
  - 401: Unauthorized
  - 403: Forbidden
  - 404: Not Found
  - 429: Too Many Requests (rate limit)

Server Errors (5xx):
  - 500: Internal Server Error
  - 502: Bad Gateway
  - 503: Service Unavailable
  - 504: Gateway Timeout

Measurement:
  - 4xx rate: sum(rate(http_requests_total{status=~"4.."})) / sum(rate(http_requests_total))
  - 5xx rate: sum(rate(http_requests_total{status=~"5.."})) / sum(rate(http_requests_total))
```

---

## 6. Error Budgets & Burn Rate Alerts

### 6.1 Error Budget Policy

| Service | Budget | Burn Rate Warning | Burn Rate Critical |
|---------|--------|-------------------|-------------------|
| Payment Service | 0.05% (30d) | > 10%/day | > 100%/day (1h budget) |
| API Gateway | 0.01% (30d) | > 10%/day | > 100%/day |
| Auth Service | 0.1% (30d) | > 10%/day | > 100%/day |
| Risk Service | 0.1% (30d) | > 10%/day | > 100%/day |

### 6.2 Burn Rate Alert Rules

```yaml
# Multi-window burn rate alerts (recommended for SLO tracking)

- alert: SLOBurnRateWarning
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h])) by (job)
      / 
      sum(rate(http_server_requests_seconds_count[1h])) by (job)
    ) 
    > (0.1 / 24 / 10)  # 10% of daily budget in 1h = 100% daily burn
  for: 3h
  labels:
    severity: warning
    category: slo
  annotations:
    summary: "SLO burn rate warning for {{ $labels.job }}"
    description: "Service {{ $labels.job }} is burning error budget at {{ $value | humanizePercentage }}/hour. 24h budget will be exhausted in 10 hours."

- alert: SLOBurnRateCritical
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[1h])) by (job)
      / 
      sum(rate(http_server_requests_seconds_count[1h])) by (job)
    ) 
    > (0.1 / 24)  # 100% daily burn rate
  for: 1h
  labels:
    severity: critical
    category: slo
  annotations:
    summary: "SLO burn rate CRITICAL for {{ $labels.job }}"
    description: "Service {{ $labels.job }} is burning error budget at {{ $value | humanizePercentage }}/hour. 24h budget will be exhausted in 1 hour!"

- alert: SLOBudgetExhausted
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[30d])) by (job)
      / 
      sum(rate(http_server_requests_seconds_count[30d])) by (job)
    ) 
    > 0.001  # 0.1% error budget
  labels:
    severity: critical
    category: slo
  annotations:
    summary: "SLO budget exhausted for {{ $labels.job }}"
    description: "Service {{ $labels.job }} has exhausted its 30-day error budget. Immediate action required."
```

### 6.3 4h22m Availability Alert

```yaml
- alert: AvailabilitySLOViolation
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{status=~"5.."}[24h])) by (job)
      / 
      sum(rate(http_server_requests_seconds_count[24h])) by (job)
    ) 
    > 0.0005  # 0.05% = 99.95% availability
  for: 5m
  labels:
    severity: critical
    category: availability
  annotations:
    summary: "Availability SLO violation for {{ $labels.job }}"
    description: "Service {{ $labels.job }} availability is below 99.95% over the last 24 hours."
```

---

## 7. Implementation Notes

### Metric Labels Required
Ensure all services expose:
- `job` or `service`: Service identifier
- `status`: HTTP status code (2xx, 4xx, 5xx)
- `uri`: Endpoint path (for detailed SLOs)

### Recording Rules
See `ops/prometheus/recording-rules.yml` for pre-computed SLO metrics.

### Dashboard Integration
Import Grafana dashboards from `ops/grafana/dashboards/` to visualize SLO compliance.

### Alert Routing
- Route SLO alerts to `#payment-alerts` (Slack)
- Route critical burn rate alerts to PagerDuty

---

## 8. Review Cadence

- **Weekly**: Review SLO performance in team standup
- **Monthly**: Analyze error budget consumption
- **Quarterly**: Review and adjust SLO targets based on historical data