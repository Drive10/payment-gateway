#!/bin/bash

set -e

echo "=============================================="
echo "  Payment Gateway - Stop Script"
echo "=============================================="

# Stop all services
echo "Stopping services..."
docker compose --profile services down
docker compose --profile infra down

# Clean up volumes (optional)
if [ "$1" == "--clean" ]; then
    echo "Cleaning up volumes..."
    docker volume rm payment-gateway_postgres_data 2>/dev/null || true
    docker volume rm payment-gateway_prometheus_data 2>/dev/null || true
    docker volume rm payment-gateway_grafana_data 2>/dev/null || true
    echo "Volumes cleaned."
fi

echo ""
echo "Payment Gateway stopped."
