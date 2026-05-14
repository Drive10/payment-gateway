#!/bin/bash
source .env

SERVICES_DIR=src
JAVA_OPTS="-Dspring.profiles.active=local"

start_service() {
    local name=$1
    local jar="$SERVICES_DIR/$name/target/$name-1.0.0.jar"
    local log="/tmp/$name.log"
    
    if [ -f "$jar" ]; then
        nohup java $JAVA_OPTS -jar "$jar" > "$log" 2>&1 &
        echo "✓ $name started (PID $!) on port $(grep -oP 'server\.port=\K\d+|grep -oP 'port: \K\d+' src/$name/src/main/resources/application-local.yml 2>/dev/null || echo '?')"
    else
        echo "✗ $name JAR not found at $jar"
    fi
}

start_service "auth-service"
start_service "simulator-service"
start_service "notification-service"
start_service "payment-service"
start_service "api-gateway"
start_service "analytics-service"
start_service "audit-service"

echo ""
echo "All services started. Check logs in /tmp/*.log"
