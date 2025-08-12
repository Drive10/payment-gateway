FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . /app
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+AlwaysPreTouch"
WORKDIR /app
COPY --from=build /app/target/*.jar /app/service.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/service.jar"]
