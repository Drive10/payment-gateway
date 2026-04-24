#!/bin/bash
set -e

echo "PayFlow Development Environment"
echo "=============================="

# Start infrastructure
echo "Starting infrastructure (PostgreSQL, Redis, Kafka)..."
docker compose --profile infra up -d
sleep 8

# Build all services
echo "Building services..."
mvn clean package -DskipTests -q

# Start payment service (core)
echo "Starting payment-service on :8083..."
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.fork=false &
PAYMENT_PID=$!
sleep 3

# Start notification service
echo "Starting notification-service on :8084..."
mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.fork=false &
NOTIF_PID=$!
sleep 3

# Start simulator service
echo "Starting simulator-service on :8086..."
mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.fork=false &
SIM_PID=$!
sleep 3

# Start frontend
echo "Starting frontend on :5173..."
cd frontend/payment-page && npm run dev &
FRONTEND_PID=$!

echo ""
echo "All services started!"
echo "  Payment Service:      http://localhost:8083"
echo "  Notification:        http://localhost:8084"
echo "  Simulator:           http://localhost:8086"
echo "  Frontend:            http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop all services"

cleanup() {
    echo "Stopping services..."
    kill $PAYMENT_PID $NOTIF_PID $SIM_PID $FRONTEND_PID 2>/dev/null || true
    docker compose down
}

trap cleanup EXIT
wait