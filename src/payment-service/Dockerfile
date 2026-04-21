# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only necessary files for dependency resolution
COPY pom.xml .
COPY common/pom.xml common/
COPY libs/common-config/pom.xml libs/common-config/
COPY src/order-service/pom.xml src/order-service/
COPY src/auth-service/pom.xml src/auth-service/
COPY src/payment-service/pom.xml src/payment-service/
COPY src/notification-service/pom.xml src/notification-service/
COPY src/simulator-service/pom.xml src/simulator-service/
COPY src/api-gateway/pom.xml src/api-gateway/

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY . .

# Build all services
RUN mvn package -DskipTests -pl libs/common-config -am && \
    mvn package -DskipTests -pl src/order-service -am && \
    mvn package -DskipTests -pl src/auth-service -am && \
    mvn package -DskipTests -pl src/payment-service -am && \
    mvn package -DskipTests -pl src/notification-service -am && \
    mvn package -DskipTests -pl src/simulator-service -am && \
    mvn package -DskipTests -pl src/api-gateway -am

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="PayFlow Team <support@payflow.dev>"
LABEL description="PayFlow Payment Service"

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

RUN mkdir -p /app/logs && \
    touch /app/logs/application.log /app/logs/error.log && \
    chown -R spring:spring /app/logs

# Copy service JAR
COPY --from=builder /build/src/payment-service/target/payment-service-1.0.0.jar app.jar
COPY wait-for.sh /wait-for.sh

RUN chmod +x /wait-for.sh && \
    chown -R spring:spring /app

USER spring:spring

EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# JVM Options for production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
    LOG_PATH=/app/logs \
    JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError" \
    JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=/app/logs/heapdump.hprof"

ENTRYPOINT ["/wait-for.sh", "postgres:5432", "--", "sh", "-c", "java $JAVA_OPTS -jar app.jar"]