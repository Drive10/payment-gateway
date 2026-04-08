#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_URL="${E2E_BASE_URL:-http://localhost:5173}"
GATEWAY_URL="${E2E_GATEWAY_URL:-http://localhost:8080}"
HEALTH_URLS="${E2E_REQUIRED_HEALTH_URLS:-$GATEWAY_URL}"

IFS=',' read -r -a health_urls <<< "$HEALTH_URLS"
for url in "${health_urls[@]}"; do
  url="$(echo "$url" | xargs)"
  if [ -z "$url" ]; then
    continue
  fi
  echo "[e2e] waiting for service health at ${url}"
  "$ROOT_DIR/scripts/wait-for-health.sh" "$url"
done

echo "[e2e] waiting for frontend at ${FRONTEND_URL}"
until curl -fsS "${FRONTEND_URL}" >/dev/null; do
  sleep 2
done

echo "[e2e] running fullstack frontend+backend Playwright spec"
cd "$ROOT_DIR/web/frontend"
E2E_BASE_URL="${FRONTEND_URL}" npm run test:e2e:fullstack
