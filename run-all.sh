#!/bin/bash
set -e

export POSTGRES_PASSWORD=payflow_dev_pass_123
export REDIS_PASSWORD=redis_dev_pass_123
export JWT_SECRET=dGhpcyBpcyBhIGp3dCBzZWNyZXQga2V5IGZvciBwYXlmbG93IGRldiBlbnZpcm9ubWVudA==
export INTERNAL_AUTH_SECRET=internal_service_secret_dev_456
export PAYMENT_WEBHOOK_SECRET=webhook_secret_dev_789
export MERCHANT_API_KEYS=merchant_key_test_001,merchant_key_test_002
export CORS_ALLOWED_ORIGINS=http://localhost:5173
export AUTH_SERVICE_URL=http://localhost:8082
export PAYMENT_SERVICE_URL=http://localhost:8083
export SIMULATOR_SERVICE_URL=http://localhost:8086
export NOTIFICATION_SERVICE_URL=http://localhost:8085
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export REDIS_HOST=localhost
export REDIS_PORT=6379
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

JAVA_OPTS="-Dspring.profiles.active=local"
BASE=/home/umeshlohot/IdeaProjects/payflow

echo "Starting PayFlow services..."

java $JAVA_OPTS -jar $BASE/src/auth-service/target/auth-service-1.0.0.jar &>/tmp/auth-service.log &
echo "  auth-service (8082) PID=$!"

sleep 3

java $JAVA_OPTS -jar $BASE/src/simulator-service/target/simulator-service-1.0.0.jar &>/tmp/simulator-service.log &
echo "  simulator-service (8086) PID=$!"

java $JAVA_OPTS -jar $BASE/src/notification-service/target/notification-service-1.0.0.jar &>/tmp/notification-service.log &
echo "  notification-service (8085) PID=$!"

java $JAVA_OPTS -jar $BASE/src/payment-service/target/payment-service-1.0.0.jar &>/tmp/payment-service.log &
echo "  payment-service (8083) PID=$!"

java $JAVA_OPTS -jar $BASE/src/api-gateway/target/api-gateway-1.0.0.jar &>/tmp/gateway-service.log &
echo "  api-gateway (8080) PID=$!"

java $JAVA_OPTS -jar $BASE/src/analytics-service/target/analytics-service-1.0.0.jar &>/tmp/analytics-service.log &
echo "  analytics-service (8087) PID=$!"

java $JAVA_OPTS -jar $BASE/src/audit-service/target/audit-service-1.0.0.jar &>/tmp/audit-service.log &
echo "  audit-service (8088) PID=$!"

echo ""
echo "All services started. Waiting 15s for readiness..."
sleep 15
echo "Check logs:"
echo "  tail -f /tmp/auth-service.log"
echo "  tail -f /tmp/payment-service.log"
echo "  tail -f /tmp/gateway-service.log"
