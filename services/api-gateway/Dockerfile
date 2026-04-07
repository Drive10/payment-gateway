# ============================================
# Multi-stage Dockerfile for API Gateway
# ============================================

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

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

# Create minimal parent POM with only api-gateway
RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
    <modelVersion>4.0.0</modelVersion>\
    <groupId>dev.payflow</groupId>\
    <artifactId>payflow</artifactId>\
    <version>1.0.0</version>\
    <packaging>pom</packaging>\
    <modules><module>libs/common</module><module>services/api-gateway</module></modules>\
    <properties><java.version>21</java.version><maven.compiler.source>21</maven.compiler.source><maven.compiler.target>21</maven.compiler.target><spring-boot.version>3.3.0</spring-boot.version><spring-cloud.version>2023.0.0</spring-cloud.version><springdoc.version>2.5.0</springdoc.version><lombok.version>1.18.30</lombok.version></properties>\
    <build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin></plugins></build>\
    <dependencyManagement><dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency><dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-dependencies</artifactId><version>${spring-cloud.version}</version><type>pom</type><scope>import</scope></dependency><dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webflux-ui</artifactId><version>${springdoc.version}</version></dependency><dependency><groupId>org.springframework.vault</groupId><artifactId>spring-vault-core</artifactId><version>3.1.0</version></dependency><dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version></dependency><dependency><groupId>org.mockito</groupId><artifactId>mockito-core</artifactId><version>5.11.0</version></dependency></dependencies></dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/api-gateway/src services/api-gateway/src
RUN mvn package -pl services/api-gateway -DskipTests -q -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app
COPY --from=build --chown=appuser:appgroup /build/services/api-gateway/target/*.jar /app/service.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/service.jar"]