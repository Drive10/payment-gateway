# ============================================
# Multi-stage Dockerfile for Auth Service
# ============================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Create minimal parent POM with only libs/common
RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
    <modelVersion>4.0.0</modelVersion>\
    <groupId>dev.payflow</groupId>\
    <artifactId>payflow</artifactId>\
    <version>1.0.0</version>\
    <packaging>pom</packaging>\
    <modules><module>libs/common</module></modules>\
    <properties><java.version>21</java.version><maven.compiler.source>21</maven.compiler.source><maven.compiler.target>21</maven.compiler.target><spring-boot.version>3.3.0</spring-boot.version></properties>\
    <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin></plugins></build>\
    <dependencyManagement><dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
RUN mvn install -pl libs/common -DskipTests -q -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

# Create minimal parent POM with only auth-service
RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
    <modelVersion>4.0.0</modelVersion>\
    <groupId>dev.payflow</groupId>\
    <artifactId>payflow</artifactId>\
    <version>1.0.0</version>\
    <packaging>pom</packaging>\
    <modules><module>libs/common</module><module>services/auth-service</module></modules>\
    <properties><java.version>21</java.version><maven.compiler.source>21</maven.compiler.source><maven.compiler.target>21</maven.compiler.target><spring-boot.version>3.3.0</spring-boot.version><spring-cloud.version>2023.0.0</spring-cloud.version><jjwt.version>0.12.5</jjwt.version><resilience4j.version>2.2.0</resilience4j.version></properties>\
    <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin></plugins></build>\
    <dependencyManagement><dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency><dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-dependencies</artifactId><version>${spring-cloud.version}</version><type>pom</type><scope>import</scope></dependency><dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency><dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency><dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency><dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-spring-boot3</artifactId><version>${resilience4j.version}</version></dependency><dependency><groupId>io.github.resilience4j</groupId><artifactId>resilience4j-ratelimiter</artifactId><version>${resilience4j.version}</version></dependency><dependency><groupId>org.springframework.vault</groupId><artifactId>spring-vault-core</artifactId><version>3.1.0</version></dependency></dependencies></dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
COPY services/auth-service/pom.xml services/auth-service/
COPY services/auth-service/src services/auth-service/src
RUN mvn package -pl services/auth-service -DskipTests -q -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/services/auth-service/target/auth-service-*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:TieredStopAtLevel=1 -Xshare:on -Dspring.main.lazy-initialization=true"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
