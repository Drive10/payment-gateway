#!/bin/bash
set -e

echo "Starting PayFlow dev environment..."

# Start infra
echo "Starting infrastructure..."
docker compose up -d
sleep 5

# Build backend
echo "Building backend..."
mvn clean package -DskipTests -q

# Start services
echo "Starting services..."
mvn spring-boot:run -pl src/auth-service,src/order-service,src/payment-service,src/notification-service,src/simulator-service -Dspring-boot.run.fork=false &
BACKEND_PID=$!

# Start frontend
echo "Starting frontend..."
cd frontend/payment-page && npm run dev &
FRONTEND_PID=$!

echo ""
echo "Services started!"
echo "  API Gateway:  http://localhost:8080"
echo "  Auth:       http://localhost:8081"
echo "  Order:      http://localhost:8082"
echo "  Payment:    http://localhost:8083"
echo "  Frontend:   http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop all services"

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; docker compose down" EXIT
wait