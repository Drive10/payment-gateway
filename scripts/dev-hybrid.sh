#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "[hybrid] starting infra + selected services in docker..."
docker compose --env-file .env.hybrid -f docker-compose.yml -f docker-compose.hybrid.yml up -d \
  --no-deps \
  postgres mongodb redis zookeeper kafka zipkin \
  payment-service notification-service simulator-service analytics-service audit-service api-gateway

echo "[hybrid] run auth-service and order-service locally using profile hybrid:"
echo "  SPRING_PROFILES_ACTIVE=hybrid mvn -pl services/auth-service spring-boot:run"
echo "  SPRING_PROFILES_ACTIVE=hybrid mvn -pl services/order-service spring-boot:run"
