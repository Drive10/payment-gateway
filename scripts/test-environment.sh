#!/bin/bash
set -e

echo "=========================================="
echo "Payment Gateway - Environment Test Script"
echo "=========================================="

export COMPOSE_PROJECT_NAME=payment-gateway

echo ""
echo "=== 1. Starting Infrastructure ==="
docker compose -f docker-compose.yml up -d vault postgres redis kafka

echo "Waiting for infrastructure..."
sleep 30

echo ""
echo "=== 2. Checking Infrastructure ==="
echo "PostgreSQL: $(docker compose -f docker-compose.yml ps postgres | grep -q healthy && echo 'UP' || echo 'DOWN')"
echo "Redis: $(docker compose -f docker-compose.yml ps redis | grep -q healthy && echo 'UP' || echo 'DOWN')"
echo "Kafka: $(docker compose -f docker-compose.yml ps kafka | grep -q healthy && echo 'UP' || echo 'DOWN')"
echo "Vault: $(curl -sf http://localhost:8200/v1/sys/health && echo 'UP' || echo 'DOWN')"

echo ""
echo "=== 3. Building All Service Images ==="
for svc in api-gateway auth-service order-service payment-service notification-service simulator-service analytics-service; do
  echo "Building $svc..."
  docker build --no-cache --build-arg SERVICE_PATH=services/$svc -t payment-gateway-$svc:latest -f Dockerfile.build . 2>&1 | tail -1
done

echo ""
echo "=== 4. Starting All Services ==="
docker compose -f docker-compose.yml up -d

echo "Waiting for services to initialize..."
sleep 60

echo ""
echo "=== 5. Service Health Check ==="
SERVICES=(
  "8081:Auth Service"
  "8082:Order Service"
  "8083:Payment Service"
  "8084:Notification Service"
  "8085:Analytics Service"
  "8086:Simulator Service"
)

for svc in "${SERVICES[@]}"; do
  port="${svc%%:*}"
  name="${svc##*:}"
  
  status=$(curl -sf -m 10 "http://localhost:$port/actuator/health" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")
  
  if [ "$status" = "UP" ]; then
    echo "✓ $name: UP"
  else
    echo "✗ $name: $status"
  fi
done

echo ""
echo "=== 6. Failed Services Logs ==="
for port in 8081 8082 8083 8084 8085 8086; do
  status=$(curl -sf -m 5 "http://localhost:$port/actuator/health" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','DOWN'))" 2>/dev/null || echo "DOWN")
  if [ "$status" != "UP" ]; then
    container=$(docker ps --format "{{.Names}}" | grep service | head -1)
    echo "--- Port $port ---"
    docker logs "$container" 2>&1 | grep -i "error\|failed" | head -3
  fi
done

echo ""
echo "=== Test Complete ==="
