FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build --chown=appuser:appgroup /app/target/*.jar /app/service.jar
USER appuser
ENTRYPOINT ["java","-XX:+UseZGC","-XX:MaxRAMPercentage=75.0","-jar","/app/service.jar"]
