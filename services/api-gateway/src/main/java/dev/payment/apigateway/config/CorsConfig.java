package dev.payment.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Origin",
            "Cache-Control",
            "Content-Type",
            "Authorization",
            "X-Timestamp",
            "X-Signature",
            "Idempotency-Key"
        ));
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Timestamp",
            "X-Signature"
        ));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
