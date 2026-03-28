FROM maven:3.9.9-eclipse-temurin-21 AS builder

ARG SERVICE_PATH
WORKDIR /build

COPY . .


RUN mvn -pl ${SERVICE_PATH} -am clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app


COPY --from=builder /build/${SERVICE_PATH}/target/*jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]