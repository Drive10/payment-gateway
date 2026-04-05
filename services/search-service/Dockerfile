FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY services/search-service/pom.xml services/search-service/
COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
RUN mvn dependency:go-offline -pl services/search-service -am || true
COPY services/search-service/src services/search-service/src
RUN mvn package -pl services/search-service -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/services/search-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD wget --quiet --tries=1 --spider http://localhost:8088/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
