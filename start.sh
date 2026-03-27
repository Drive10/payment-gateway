#!/bin/bash
set -e

# Fintech Payment Platform - Startup Script
# Usage: ./start.sh [full|infra|hybrid|down]

MODE=${1:-help}

# Auto-provision .env if it doesn't exist
if [ ! -f .env ]; then
  echo "=> .env file not found. Creating from .env.example..."
  cp .env.example .env
fi

case "$MODE" in
  full)
    echo "=> Starting Full Docker Environment (All Services + Infra)..."
    docker compose --profile services up -d --build
    ;;
  infra)
    echo "=> Starting Full Local Dev Environment (Infra Only)..."
    docker compose --profile infra up -d
    echo "=> Now run your Java microservices via your IDE with SPRING_PROFILES_ACTIVE=dev"
    ;;
  hybrid)
    echo "=> Starting Hybrid Mode (Infra + Other Services, scaling payment-service to 0)..."
    # Starting standard docker profile but shutting down payment-service for local development
    docker compose --profile services up -d --build --scale payment-service=0
    echo "=> Now run payment-service via your IDE with SPRING_PROFILES_ACTIVE=dev"
    echo "=> Ensure PAYMENT_SERVICE_URL=http://host.docker.internal:8084 is enabled in .env!"
    ;;
  down)
    echo "=> Shutting down all containers and removing volumes..."
    docker compose down -v
    ;;
  *)
    echo "Usage: ./start.sh [command]"
    echo ""
    echo "Commands:"
    echo "  full   - Build and run all infrastructure and microservices in Docker."
    echo "  infra  - Run ONLY infrastructure (Postgres, Redis, Kafka, Observability)."
    echo "  hybrid - Run infra + all microservices EXCEPT payment-service (scale=0)."
    echo "  down   - Stop everything and remove volumes."
    exit 1
    ;;
esac

echo "=> Done."
