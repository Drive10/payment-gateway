#!/bin/bash

mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local \
  > /tmp/payment.log 2>&1 &

mvn spring-boot:run -pl src/auth-service -Dspring-boot.run.profiles=local \
  > /tmp/auth.log 2>&1 &

mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local \
  > /tmp/simulator.log 2>&1 &

mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.profiles=local \
  > /tmp/notification.log 2>&1 &

mvn spring-boot:run -pl src/analytics-service -Dspring-boot.run.profiles=local \
  > /tmp/analytics.log 2>&1 &

mvn spring-boot:run -pl src/audit-service -Dspring-boot.run.profiles=local \
  > /tmp/audit.log 2>&1 &

mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local \
  > /tmp/gateway.log 2>&1 &

echo "All services started"