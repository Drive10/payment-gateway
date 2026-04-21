# Service Template

Use this template to create new PayFlow services.

## Usage

```bash
# 1. Copy template to new service
cp -r templates/service-template src/new-service-name

# 2. Update pom.xml:
#    - Change artifactId from "SERVICE_NAME" to your service name
#    - Update name and description

# 3. Rename application class
mv src/main/java/dev/payment/template/TemplateApplication.java \
   src/main/java/dev/payment/yourname/YourServiceApplication.java

# 4. Update application.yml with correct port
```

## What's Included

- Common dependencies (common, common-config)
- Spring Boot starters (web, jpa, security, actuator)
- Database setup with PostgreSQL + Flyway
- Prometheus metrics
- Resilience4j
- Testcontainers setup

## Configuration

The service uses centralized config from `common-config`. Override as needed:

```yaml
payflow:
  database:
    enabled: true  # or false if no DB needed
  redis:
    enabled: true
  kafka:
    enabled: true
```

## Port Allocation

| Service | Port |
|---------|------|
| api-gateway | 8080 |
| auth-service | 8081 |
| order-service | 8082 |
| payment-service | 8083 |
| notification-service | 8084 |
| simulator-service | 8086 |
| analytics-service | 8089 |
| audit-service | 8090 |
| **New service** | **8091+** |