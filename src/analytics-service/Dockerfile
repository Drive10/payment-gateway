FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/analytics-service-1.0.0.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
