FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
    <modelVersion>4.0.0</modelVersion>\
    <groupId>dev.payflow</groupId>\
    <artifactId>payflow</artifactId>\
    <version>1.0.0</version>\
    <packaging>pom</packaging>\
    <modules><module>libs/common</module></modules>\
    <properties>\
        <java.version>21</java.version>\
        <maven.compiler.source>21</maven.compiler.source>\
        <maven.compiler.target>21</maven.compiler.target>\
        <spring-boot.version>3.3.0</spring-boot.version>\
    </properties>\
    <build>\
        <plugins>\
            <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin>\
        </plugins>\
    </build>\
    <dependencyManagement>\
        <dependencies>\
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency>\
        </dependencies>\
    </dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
COPY libs/common/src libs/common/src
RUN mvn install -pl libs/common -DskipTests -q

RUN printf '<?xml version="1.0" encoding="UTF-8"?>\
<project xmlns="http://maven.apache.org/POM/4.0.0">\
    <modelVersion>4.0.0</modelVersion>\
    <groupId>dev.payflow</groupId>\
    <artifactId>payflow</artifactId>\
    <version>1.0.0</version>\
    <packaging>pom</packaging>\
    <modules><module>services/search-service</module></modules>\
    <properties>\
        <java.version>21</java.version>\
        <maven.compiler.source>21</maven.compiler.source>\
        <maven.compiler.target>21</maven.compiler.target>\
        <spring-boot.version>3.3.0</spring-boot.version>\
    </properties>\
    <build>\
        <plugins>\
            <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version><configuration><source>21</source><target>21</target></configuration></plugin>\
        </plugins>\
    </build>\
    <dependencyManagement>\
        <dependencies>\
            <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency>\
        </dependencies>\
    </dependencyManagement>\
</project>' > pom.xml

COPY libs/common/pom.xml libs/common/
RUN mvn install -pl libs/common -DskipTests -q || true
COPY services/search-service/pom.xml services/search-service/
COPY services/search-service/src services/search-service/src
RUN mvn package -pl services/search-service -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /build/services/search-service/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8086
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]