package dev.payment.common.config;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(name = "ch.qos.logback.classic.PatternLayout")
public class LoggingPatternConfig {

    @Bean
    public Map<String, String> loggingPatterns() {
        Map<String, String> patterns = new HashMap<>();

        patterns.put("CONSOLE_LOG_PATTERN",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{correlationId:-}] %highlight(%-5level) %cyan(%logger{36}) - %msg%n");

        patterns.put("FILE_LOG_PATTERN",
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{correlationId:-}] %-5level %logger{36} - %msg%n");

        patterns.put("JSON_LOG_PATTERN",
                "{\"timestamp\":\"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}\",\"level\":\"%level\",\"logger\":\"%logger{36}\",\"correlationId\":\"%X{correlationId}\",\"message\":\"%msg\"}");

        return patterns;
    }
}