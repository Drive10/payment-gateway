#!/bin/bash
# PayFlow Development Helper
# Usage: ./scripts/up.sh [dev|staging|prod]

set -e

ENV=${1:-dev}

case "$ENV" in
  dev)
    cp -n .env.development .env 2>/dev/null || true
    echo "Starting PayFlow (development)..."
    docker compose up -d
    ;;
  staging)
    echo "Starting PayFlow (staging)..."
    docker compose -f docker-compose.yml -f docker-compose.staging.yml --env-file .env.staging up -d
    ;;
  prod)
    echo "Starting PayFlow (production)..."
    docker compose -f docker-compose.yml -f docker-compose.production.yml --env-file .env.production up -d
    ;;
  *)
    echo "Usage: ./scripts/up.sh [dev|staging|prod]"
    exit 1
    ;;
esac

echo "Done! Services starting..."
docker compose ps