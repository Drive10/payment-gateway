FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY services/analytics-service/pom.xml services/analytics-service/
COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
RUN mvn dependency:go-offline -pl services/analytics-service -am || true
COPY services/analytics-service/src services/analytics-service/src
RUN mvn package -pl services/analytics-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/services/analytics-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD wget --quiet --tries=1 --spider http://localhost:8086/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
