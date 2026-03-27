# Operations

## Secrets

`.env` is for local development only.

Production deployments should inject secrets through platform-native secret stores:

- GitHub Actions secrets for CI/CD
- Kubernetes Secrets for runtime delivery
- Docker secrets or mounted config trees where Kubernetes is not yet available

Recommended sensitive values:

- `JWT_SECRET_KEY`
- `RAZORPAY_WEBHOOK_SECRET`
- database passwords
- registry credentials

## Kubernetes Readiness

The services already expose actuator health endpoints. For Kubernetes:

- use `/actuator/health/liveness` for liveness checks
- use `/actuator/health/readiness` for readiness checks
- externalize non-secret config through ConfigMaps
- inject secrets through Secrets, not `.env`
- sample manifests are available under [ops/k8s](../ops/k8s/README.md)

## Hybrid Development

- Docker default path: infra + platform services
- local override path: run one service locally with the `local` Spring profile
- full Docker path: enable `full` and `optional` profiles
