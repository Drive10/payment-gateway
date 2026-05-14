#!/bin/bash
set -e

MODE="${1:-local}"

if [ "$MODE" != "local" ] && [ "$MODE" != "docker" ]; then
    echo "Usage: $0 [local|docker]"
    echo "  local  - Services run locally, infra in Docker"
    echo "  docker - Full docker setup (all in Docker)"
    exit 1
fi

# ── Prerequisites ────────────────────────────────────────────
echo ""
echo "═══ PayFlow Dev Environment ═══"
echo ""

if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

if ! command -v mvn &> /dev/null && [ "$MODE" = "local" ]; then
    echo "❌ Maven not found. Install Maven or use: $0 docker"
    exit 1
fi

# ── Infrastructure ──────────────────────────────────────────
echo "📦 Starting infrastructure (PostgreSQL, Redis, Kafka)..."
docker compose up -d

echo -n "⏳ Waiting for infrastructure"
for i in $(seq 1 15); do
    pg_ready=$(docker compose exec -T postgres pg_isready -U payflow 2>/dev/null && echo 1 || echo 0)
    redis_ok=$(docker compose exec -T redis redis-cli -a "redis_dev_pass_123" ping 2>/dev/null | grep -q PONG && echo 1 || echo 0)
    kafka_ok=$(docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 2>/dev/null | head -1 | grep -q kafka && echo 1 || echo 0)
    total=$((pg_ready + redis_ok + kafka_ok))
    echo -n " [$total/3]"
    [ "$total" -ge 3 ] && break
    sleep 3
done
echo ""

# ── Build ───────────────────────────────────────────────────
if [ "$MODE" = "local" ]; then
    echo -n "🔨 Building services..."
    mvn clean package -DskipTests -q -T 4C
    echo " done"
fi

# ── Start Services ──────────────────────────────────────────
if [ "$MODE" = "local" ]; then
    echo ""
    echo "▶️  Starting all services..."

    SERVICES=(
        "auth-service:8082"
        "payment-service:8083"
        "simulator-service:8086"
        "notification-service:8085"
        "analytics-service:8087"
        "audit-service:8088"
        "api-gateway:8080"
    )

    for entry in "${SERVICES[@]}"; do
        name="${entry%%:*}"
        port="${entry##*:}"
        log_name="${name%-service}"
        [ "$name" = "api-gateway" ] && log_name="gateway"

        mvn spring-boot:run -pl "src/$name" -Dspring-boot.run.profiles=local > "/tmp/$log_name.log" 2>&1 &
        echo "   ✓ $name (:$port)"
        sleep 1
    done

    echo ""
    echo -n "⏳ Waiting for services"
    for i in $(seq 1 20); do
        for entry in "${SERVICES[@]}"; do
            port="${entry##*:}"
            log_name="${entry%%:*}"
            log_name="${log_name%-service}"
            [ "$log_name" = "api-gateway" ] && log_name="gateway"
            if [ ! -f "/tmp/$log_name.ready" ] && grep -q "Started" "/tmp/$log_name.log" 2>/dev/null; then
                touch "/tmp/$log_name.ready"
                echo -n " [$port]"
            fi
        done
        ready=$(ls /tmp/{auth,payment,simulator,notification,analytics,audit,gateway}.ready 2>/dev/null | wc -l)
        [ "$ready" -ge 7 ] && break
        sleep 2
    done
    echo ""
    echo "   All services ready"

    # ── Frontend ──────────────────────────────────────────────
    echo ""
    echo "🎨 Starting Frontend..."
    cd frontend/payment-page
    if [ -d node_modules ]; then
        npm run dev > /tmp/frontend.log 2>&1 &
    else
        echo "   Installing frontend dependencies (first time)..."
        npm install --silent > /dev/null 2>&1
        npm run dev > /tmp/frontend.log 2>&1 &
    fi
    cd "$OLDPWD"

elif [ "$MODE" = "docker" ]; then
    echo ""
    echo "🐳 Building and starting Docker containers..."
    docker compose up -d --build
fi

# ── Verify ──────────────────────────────────────────────────
echo ""
echo -n "⏳ Verifying endpoints"
for i in $(seq 1 10); do
    gw=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/ 2>/dev/null || echo "000")
    auth=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/auth/login -X POST -H "Content-Type: application/json" -d '{"email":"dev@test.com","password":"Password123"}' 2>/dev/null || echo "000")
    [ "$gw" != "000" ] && [ "$auth" != "000" ] && break
    sleep 2
done

echo ""
echo ""
echo "═══ ✅ PayFlow is running ═══"
echo ""
echo "📍 URLs:"
echo "   Frontend:     http://localhost:5173"
echo "   API Gateway:  http://localhost:8080"
echo "   Auth:         http://localhost:8082"
echo "   Payment:      http://localhost:8083"
echo "   Swagger:      http://localhost:8083/swagger-ui.html"
echo ""
echo "🔑 Test Accounts (all password: Password123):"
echo "   Admin:     admin@payflow.dev"
echo "   Merchant:  merchant@test.com"
echo "   Customer:  dev@test.com"
echo ""
echo "💡 Quick test:"
echo "  TOKEN=\$(curl -s http://localhost:8082/auth/login -H 'Content-Type: application/json'"
echo "    -d '{\"email\":\"dev@test.com\",\"password\":\"Password123\"}'"
echo "    | grep -o '\"accessToken\":\"[^\"]*\"' | cut -d'\"' -f4)"
echo ""
echo "  curl -s http://localhost:8080/api/payments/list -H \"Authorization: Bearer \$TOKEN\""
echo ""
echo "Press Ctrl+C to stop all services"

# ── Cleanup on exit ─────────────────────────────────────────
cleanup() {
    echo ""
    echo "🛑 Stopping..."
    kill $(jobs -p) 2>/dev/null
    docker compose down 2>/dev/null
    rm -f /tmp/{auth,payment,simulator,notification,analytics,audit,gateway}.ready
    echo "Done."
}
trap cleanup SIGINT SIGTERM
wait
