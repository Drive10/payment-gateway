#!/bin/bash

set -e

echo "🚀 PayFlow Dev Setup"
echo "===================="

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

# Start infrastructure
echo ""
echo "📦 Starting infrastructure (PostgreSQL, Redis, Kafka)..."
docker compose up -d

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
sleep 10

# Build
echo ""
echo "🔨 Building services..."
mvn clean package -DskipTests -q

# Start services in background
echo ""
echo "▶️  Starting services..."

# Payment Service
mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local > /tmp/payment.log 2>&1 &
echo "   ✓ Payment Service (8083)"

# Auth Service
mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local > /tmp/auth.log 2>&1 &
echo "   ✓ Auth Service (8082)"

# Simulator Service
mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local > /tmp/simulator.log 2>&1 &
echo "   ✓ Simulator Service (8086)"

# API Gateway
mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local > /tmp/gateway.log 2>&1 &
echo "   ✓ API Gateway (8080)"

# Wait for services to start
echo ""
echo "⏳ Waiting for services to be ready..."
sleep 25

# Create test merchant if not exists
echo ""
echo "👤 Setting up test merchant..."
MERCHANT_RESPONSE=$(curl -s -X POST http://localhost:8082/merchant/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@merchant.com",
    "password": "password123",
    "businessName": "Test Merchant",
    "webhookUrl": "https://test.com/webhook"
  }' 2>/dev/null || echo "null")

if [ "$MERCHANT_RESPONSE" != "null" ]; then
  API_KEY=$(docker compose exec -T postgres psql -U payflow -d payflow -t -c "SELECT api_key FROM public.merchants WHERE email='test@merchant.com';" 2>/dev/null | xargs || echo "")
  if [ -n "$API_KEY" ]; then
    echo "   ✓ Merchant created: test@merchant.com"
    echo "   ✓ API Key: $API_KEY"
  fi
fi

# Frontend
echo ""
echo "🎨 Starting Frontend..."
cd frontend/payment-page && npm run dev > /tmp/frontend.log 2>&1 &
echo "   ✓ Frontend (5173)"

echo ""
echo "===================="
echo "✅ All services started!"
echo ""
echo "📍 URLs:"
echo "   Frontend:        http://localhost:5173"
echo "   Payment API:    http://localhost:8083"
echo "   Auth API:       http://localhost:8082"
echo "   API Gateway:    http://localhost:8080"
echo "   Swagger:        http://localhost:8083/swagger-ui.html"
echo ""
echo "🔑 Test API Key: $API_KEY"
echo ""
echo "💡 Payment Flow:"
echo "   1. Create order: POST /api/payments/create-order"
echo "   2. Authorize:    POST /api/payments/{id}/authorize-pending"
echo "   3. Authorize:    POST /api/payments/{id}/authorize"
echo "   4. Capture:     POST /api/payments/{id}/capture"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for interrupt
trap "docker compose down 2>/dev/null; exit" SIGINT SIGTERM
wait