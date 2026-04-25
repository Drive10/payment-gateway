FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/audit-service-1.0.0.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
