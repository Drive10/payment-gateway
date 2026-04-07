# DevOps Rules

> Docker, Kubernetes, and CI/CD standards for PayFlow

---

## 1. Docker Standards

### Dockerfile Template

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY settings.xml .m2/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/payment-service/src services/payment-service/src
RUN mvn -B -DskipTests -f services/payment-service/pom.xml clean package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/payment-service/target/*.jar app.jar

EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose

```yaml
services:
  payment-service:
    build:
      context: .
      dockerfile: services/payment-service/Dockerfile
    ports:
      - "8083:8083"
    environment:
      - DB_HOST=postgres
      - DB_PORT=5432
      - REDIS_HOST=redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - payflow-network

networks:
  payflow-network:
    driver: bridge
```

---

## 2. Kubernetes Standards

### Deployment Template

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  labels:
    app: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
        - name: payment-service
          image: ghcr.io/drive10/payment-service:latest
          ports:
            - containerPort: 8083
          env:
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: payflow-config
                  key: db-host
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8083
            initialDelaySeconds: 10
            periodSeconds: 5
```

---

## 3. CI/CD Pipeline

### GitHub Actions

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      
      - name: Build
        run: mvn clean verify
      
      - name: Run Tests
        run: mvn test
      
      - name: Security Scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          format: 'sarif'
      
      - name: Build Docker
        if: github.event_name == 'push'
        run: docker build -t payment-service:${{ github.sha }} .
```

---

## 4. Monitoring

### Prometheus Metrics

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'payment-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['payment-service:8083']
```

### Grafana Dashboards

- JVM metrics (memory, GC, threads)
- HTTP request metrics (latency, error rate)
- Business metrics (payments, orders)
- Infrastructure (CPU, memory, disk)

---

## 5. Health Checks

### Spring Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

---

## Quick Reference

| Area | Standard |
|------|----------|
| Docker | Multi-stage build, Alpine base |
| K8s | Resource limits, liveness/readiness |
| CI/CD | Build → Test → Security → Deploy |
| Monitoring | Prometheus + Grafana |
| Health | Spring Actuator + probes |