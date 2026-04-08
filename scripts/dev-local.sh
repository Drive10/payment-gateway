#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
  ENV_FILE=".env.example"
fi

echo "[local] starting infrastructure in docker..."
docker compose --env-file "$ENV_FILE" up -d postgres mongodb redis zookeeper kafka zipkin loki promtail prometheus grafana

echo "[local] all services should run on host with SPRING_PROFILES_ACTIVE=local"
echo "Example:"
echo "  SPRING_PROFILES_ACTIVE=local mvn -pl services/auth-service spring-boot:run"
