FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
RUN mkdir -p /app/logs && \
    touch /app/logs/application.log /app/logs/error.log && \
    chown -R spring:spring /app/logs
COPY target/payment-service-1.0.0.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" LOG_PATH=/app/logs
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]