#!/bin/bash

echo "=========================================="
echo "PayFlow - Start Services (Non-Docker)"
echo "=========================================="

# Start infrastructure
echo "Starting Infrastructure Services..."
echo "Please ensure PostgreSQL (5432), Redis (6379), Kafka (9092) are running"

# Start Auth Service
echo "Starting Auth Service (8081)..."
cd services/auth-service
mvn spring-boot:run &
AUTH_PID=$!

# Start API Gateway
echo "Starting API Gateway (8080)..."
cd ../api-gateway
mvn spring-boot:run &
GATEWAY_PID=$!

# Start Order Service
echo "Starting Order Service (8082)..."
cd ../order-service
mvn spring-boot:run &
ORDER_PID=$!

# Start Payment Service
echo "Starting Payment Service (8083)..."
cd ../payment-service
mvn spring-boot:run &
PAYMENT_PID=$!

# Start Notification Service
echo "Starting Notification Service (8084)..."
cd ../notification-service
mvn spring-boot:run &
NOTIF_PID=$!

# Start Simulator Service
echo "Starting Simulator Service (8086)..."
cd ../simulator-service
mvn spring-boot:run &
SIM_PID=$!

# Start Dashboard Service
echo "Starting Dashboard Service (3001)..."
cd ../../../services/dashboard-service
npm run start &
DASHBOARD_PID=$!

# Start Dashboard Frontend
echo "Starting Dashboard Frontend (3000)..."
cd ../../../web/dashboard
npm run dev &
FRONTEND_PID=$!

echo ""
echo "Services started!"
echo "Dashboard Frontend: http://localhost:3000"
echo "API Gateway: http://localhost:8080"
echo "Dashboard API: http://localhost:3001"
echo ""
echo "Press Ctrl+C to stop all services"

# Wait for Ctrl+C
trap "kill $AUTH_PID $GATEWAY_PID $ORDER_PID $PAYMENT_PID $NOTIF_PID $SIM_PID $DASHBOARD_PID $FRONTEND_PID 2>/dev/null; exit" SIGINT
wait