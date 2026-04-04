# Kubernetes Canary Deployment Configuration
# Usage: kubectl apply -k k8s/overlays/production/

## Canary Strategy

1. **Initial**: 10% traffic to canary, 90% to stable
2. **Analysis**: Monitor error rate, latency, success rate
3. **Promote**: If metrics pass, gradually increase traffic
4. **Rollback**: If metrics fail, automatically rollback

## Deployment Flow

```
stable (v1.0.0) ──┐
                   ├── Ingress (90/10 split)
canary (v1.1.0) ──┘
```

### Step 1: Deploy Canary
```bash
# Deploy new version as canary
kubectl apply -f k8s/base/canary-deployment.yaml
kubectl apply -f k8s/base/canary-service.yaml
```

### Step 2: Configure Traffic Split
```bash
# Using Istio VirtualService
kubectl apply -f k8s/base/canary-virtualservice.yaml
```

### Step 3: Monitor Metrics
```bash
# Check canary metrics
kubectl get canary payment-gateway -n payflow-production
```

### Step 4: Promote or Rollback
```bash
# Promote canary to stable
kubectl apply -f k8s/base/promote-canary.yaml

# Or rollback
kubectl delete -f k8s/base/canary-deployment.yaml
```

## Canary Metrics Thresholds

| Metric | Threshold | Action |
|--------|-----------|--------|
| Error Rate | < 1% | Continue |
| p99 Latency | < 500ms | Continue |
| Success Rate | > 99% | Continue |
| Payment Success Rate | > 99.5% | Continue |

## Rollback Triggers

Automatic rollback if any of:
- Error rate > 5% for 2 minutes
- p99 latency > 1s for 2 minutes
- Payment success rate < 95% for 1 minute
- Health check fails 3 consecutive times
