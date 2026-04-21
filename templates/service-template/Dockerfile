# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy POM files for dependency resolution
COPY pom.xml .
RUN for dir in common libs/common-config src/order-service src/auth-service src/notification-service src/simulator-service; do \
    [ -f "$dir/pom.xml" ] && cp "$dir/pom.xml" "$dir/" || true; \
done

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY . .

# Build service
ARG SERVICE_NAME
RUN mvn package -DskipTests -pl libs/common-config -am && \
    mvn package -DskipTests -pl src/${SERVICE_NAME} -am

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="PayFlow Team <support@payflow.dev>"

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

RUN mkdir -p /app/logs && \
    touch /app/logs/application.log /app/logs/error.log && \
    chown -R spring:spring /app/logs

# Copy service JAR (passed as build arg)
ARG SERVICE_NAME
COPY --from=builder /build/src/${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar app.jar
COPY wait-for.sh /wait-for.sh

RUN chmod +x /wait-for.sh && \
    chown -R spring:spring /app

USER spring:spring

# Service-specific port (passed as build arg)
ARG SERVICE_PORT
EXPOSE ${SERVICE_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${SERVICE_PORT}/actuator/health || exit 1

# JVM Options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
    LOG_PATH=/app/logs

ENTRYPOINT ["/wait-for.sh", "postgres:5432", "--", "sh", "-c", "java $JAVA_OPTS -jar app.jar"]