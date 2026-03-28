FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests -pl services/payment-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/payment-service/target/*-exec.jar /app/app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
