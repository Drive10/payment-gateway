#!/bin/bash

set -e

echo "=============================================="
echo "  Payment Gateway - Quick Start Script"
echo "=============================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker Desktop first."
    exit 1
fi

# Check if ports are available
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo "Warning: Port $1 is already in use"
        return 1
    fi
    return 0
}

echo ""
echo "Checking ports..."
check_port 8080 || echo "  - API Gateway (8080) may conflict"
check_port 5432 || echo "  - PostgreSQL (5432) may conflict"
check_port 9092 || echo "  - Kafka (9092) may conflict"

echo ""
echo "Starting services..."

# Create network if not exists
docker network create fintech-network 2>/dev/null || true

# Start infrastructure
echo "  Starting infrastructure (PostgreSQL, Kafka, Redis)..."
docker compose --profile infra up -d

# Wait for infrastructure to be ready
echo "  Waiting for infrastructure to be ready..."
sleep 10

# Start all services
echo "  Starting all services..."
docker compose --profile services up -d --build

echo ""
echo "Waiting for services to be healthy..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "=============================================="
        echo "  Payment Gateway Started Successfully!"
        echo "=============================================="
        echo ""
        echo "Access Points:"
        echo "  - API Gateway:     http://localhost:8080"
        echo "  - Swagger UI:      http://localhost:8080/swagger-ui.html"
        echo "  - Grafana:        http://localhost:3002 (admin/admin123)"
        echo "  - Prometheus:      http://localhost:9090"
        echo "  - Jaeger:         http://localhost:16686"
        echo ""
        echo "Demo Credentials:"
        echo "  - Email:    demo@example.com"
        echo "  - Password: Demo123!"
        echo ""
        echo "Quick Test:"
        echo "  curl http://localhost:8080/api/v1/health"
        echo ""
        break
    fi
    echo "  Waiting... ($i/30)"
    sleep 5
done

if ! curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo ""
    echo "Warning: Services may not be fully ready. Check logs with:"
    echo "  docker compose logs -f"
fi
