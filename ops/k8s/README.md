# Kubernetes Prep

These manifests are intentionally minimal and production-oriented:

- `namespace.yaml`: isolated namespace for the platform
- `configmap.yaml`: non-secret runtime configuration
- `secret.example.yaml`: example secret shape; do not commit real values
- `payment-service.yaml`: deployment and service with readiness/liveness probes
- `api-gateway.yaml`: deployment and service with readiness/liveness probes

Apply in this order:

```bash
kubectl apply -f ops/k8s/namespace.yaml
kubectl apply -f ops/k8s/configmap.yaml
kubectl apply -f ops/k8s/secret.example.yaml
kubectl apply -f ops/k8s/payment-service.yaml
kubectl apply -f ops/k8s/api-gateway.yaml
```

For real environments, replace example image tags and wire PostgreSQL, Kafka, Redis, Zipkin, and ingress through your cluster platform.
