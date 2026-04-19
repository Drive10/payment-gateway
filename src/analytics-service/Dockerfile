# ============================================
# Multi-stage Dockerfile for Analytics Service
# ============================================

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Create minimal parent POM for analytics-service build
RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
<modelVersion>4.0.0</modelVersion>\
<groupId>dev.payflow</groupId>\
<artifactId>payflow</artifactId>\
<version>1.0.0</version>\
<packaging>pom</packaging>\
<modules>\
<module>libs/common</module>\
<module>services/analytics-service</module>\
</modules>\
<properties>\
<java.version>21</java.version>\
<maven.compiler.source>21</maven.compiler.source>\
<maven.compiler.target>21</maven.compiler.target>\
<spring-boot.version>3.2.5</spring-boot.version>\
<spring-cloud.version>2023.0.0</spring-cloud.version>\
<jjwt.version>0.12.5</jjwt.version>\
</properties>\
<build>\
<plugins>\
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin>\
</plugins>\
</build>\
<dependencyManagement>\
<dependencies>\
<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency>\
<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-dependencies</artifactId><version>${spring-cloud.version}</version><type>pom</type><scope>import</scope></dependency>\
<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><version>42.7.1</version></dependency>\
</dependencies>\
</dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src

COPY services/analytics-service/pom.xml services/analytics-service/
COPY services/analytics-service/src services/analytics-service/src

RUN mvn install -pl libs/common -DskipTests -q -Dmaven.compiler.source=21 -Dmaven.compiler.target=21 && \
    mvn package -pl services/analytics-service -DskipTests -q -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/services/analytics-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8089
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
COPY wait-for.sh /wait-for.sh
RUN chmod +x /wait-for.sh
ENTRYPOINT ["/wait-for.sh", "postgres:5432", "--", "sh", "-c", "java $JAVA_OPTS -jar app.jar"]