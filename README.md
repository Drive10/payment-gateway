# Cloud-Native Payment Gateway — Modernized (Java 21, Spring Boot 3.3.2)

Production-grade, microservice-based payment gateway with DDD, Kafka SAGA, observability, and secure DevSecOps.

> Original app (if provided) is under `legacy/` for reference.

## Quick Start
```bash
./mvnw -q -DskipTests package
docker compose -f deploy/local/docker-compose.yml up -d
java -jar services/payment-service/target/payment-service.jar
```
