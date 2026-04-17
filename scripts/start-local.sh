#!/bin/bash

echo "=========================================="
echo "PayFlow - Start Services (Non-Docker)"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "Starting Infrastructure Services..."
echo "Please ensure PostgreSQL (5432), Redis (6379), Kafka (9092) are running"
echo ""

cd services/auth-service
echo "Starting Auth Service (8081)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
AUTH_PID=$!
echo "Auth Service PID: $AUTH_PID"

cd ../api-gateway
echo "Starting API Gateway (8080)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
GATEWAY_PID=$!
echo "API Gateway PID: $GATEWAY_PID"

cd ../order-service
echo "Starting Order Service (8082)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
ORDER_PID=$!
echo "Order Service PID: $ORDER_PID"

cd ../payment-service
echo "Starting Payment Service (8083)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
PAYMENT_PID=$!
echo "Payment Service PID: $PAYMENT_PID"

cd ../notification-service
echo "Starting Notification Service (8084)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
NOTIF_PID=$!
echo "Notification Service PID: $NOTIF_PID"

cd ../simulator-service
echo "Starting Simulator Service (8086)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
SIM_PID=$!
echo "Simulator Service PID: $SIM_PID"

cd ../analytics-service
echo "Starting Analytics Service (8089)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
ANALYTICS_PID=$!
echo "Analytics Service PID: $ANALYTICS_PID"

cd ../audit-service
echo "Starting Audit Service (8090)..."
mvn spring-boot:run -Dspring-boot.run.profiles=local &
AUDIT_PID=$!
echo "Audit Service PID: $AUDIT_PID"

echo ""
echo "=========================================="
echo "Services started!"
echo "=========================================="
echo "API Gateway: http://localhost:8080"
echo "Auth Service: http://localhost:8081"
echo "Order Service: http://localhost:8082"
echo "Payment Service: http://localhost:8083"
echo "Notification Service: http://localhost:8084"
echo "Simulator Service: http://localhost:8086"
echo "Analytics Service: http://localhost:8089"
echo "Audit Service: http://localhost:8090"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

PIDS=($AUTH_PID $GATEWAY_PID $ORDER_PID $PAYMENT_PID $NOTIF_PID $SIM_PID $ANALYTICS_PID $AUDIT_PID)

cleanup() {
    echo ""
    echo "Stopping all services..."
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            echo "Stopped PID: $pid"
        fi
    done
    exit 0
}

trap cleanup SIGINT SIGTERM

wait