# Cloud-Native Payment Gateway (Clean Build)

This repo is ready for local dev and production deployments without custom Maven repos.

## Prereqs
- Java 21
- Maven 3.9+
- Docker (for local infra)
- kubectl + Helm (for prod)

## Local Development
```bash
# 1) Start local infra
docker compose -f deploy/local/docker-compose.yml up -d

# 2) Build
mvn -U clean install

# 3) Run payment service
java -jar services/payment-service/target/payment-service.jar
# or
mvn -pl services/payment-service spring-boot:run
```

### API Smoke Test
```bash
curl -X POST http://localhost:8081/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"o-1","customerId":"c-1","currency":"USD","amount":100.00}'
```

## Production (Kubernetes with Helm)
```bash
# Build & push image
docker build -t ghcr.io/<org>/<repo>/payment-service:latest services/payment-service
docker push ghcr.io/<org>/<repo>/payment-service:latest

# Deploy
helm upgrade --install pgw deploy/helm/payment-gateway \
  --set global.image.repository=ghcr.io/<org>/<repo> \
  --set global.image.tag=latest
```

## CI (GitHub Actions)
- `.github/workflows/build-test.yml` builds and tests on push/PR.
- `.github/workflows/docker-publish.yml` publishes images on version tags.
- `.github/workflows/deploy-eks.yml` provides a manual Helm deploy (configure AWS OIDC role).
