#!/bin/bash
source .env

env \
  JWT_SECRET="$JWT_SECRET" \
  INTERNAL_AUTH_SECRET="$INTERNAL_AUTH_SECRET" \
  PAYMENT_WEBHOOK_SECRET="$PAYMENT_WEBHOOK_SECRET" \
  MERCHANT_API_KEYS="$MERCHANT_API_KEYS" \
  POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  REDIS_PASSWORD="$REDIS_PASSWORD" \
  CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
  mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local \
  > /tmp/payment.log 2>&1 &

env \
  JWT_SECRET="$JWT_SECRET" \
  INTERNAL_AUTH_SECRET="$INTERNAL_AUTH_SECRET" \
  CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
  POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  REDIS_PASSWORD="$REDIS_PASSWORD" \
  mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local \
  > /tmp/auth.log 2>&1 &

mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local \
  > /tmp/simulator.log 2>&1 &

mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.profiles=local \
  > /tmp/notification.log 2>&1 &

mvn spring-boot:run -pl src/merchant-backend -Dspring-boot.run.profiles=local \
  > /tmp/merchant.log 2>&1 &

env \
  JWT_SECRET="$JWT_SECRET" \
  INTERNAL_AUTH_SECRET="$INTERNAL_AUTH_SECRET" \
  CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS" \
  REDIS_PASSWORD="$REDIS_PASSWORD" \
  mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local \
  > /tmp/gateway.log 2>&1 &

echo "All services started"