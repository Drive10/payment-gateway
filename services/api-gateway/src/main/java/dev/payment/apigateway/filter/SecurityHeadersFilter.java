package dev.payment.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class SecurityHeadersFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".ico",
            ".svg", ".woff", ".woff2", ".ttf", ".eot", ".map"
    );

    private static final Set<String> HEADERS_TO_REMOVE = Set.of("Server", "X-Powered-By");

    private final boolean devProfile;

    public SecurityHeadersFilter(@Value("${spring.profiles.active:}") String activeProfiles) {
        this.devProfile = activeProfiles.contains("dev");
        log.info("SecurityHeadersFilter initialized (dev profile: {})", devProfile);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            addSecurityHeaders(headers, exchange);
            removeInformationHeaders(headers);
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    private void addSecurityHeaders(HttpHeaders headers, ServerWebExchange exchange) {
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("X-XSS-Protection", "0");
        headers.set("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(self)");
        headers.set("Cross-Origin-Opener-Policy", "same-origin");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");

        if (!devProfile) {
            headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        } else {
            log.debug("HSTS header skipped for development profile");
        }

        String path = exchange.getRequest().getURI().getPath();
        if (!isStaticResource(path)) {
            headers.set("Cache-Control", "no-store, no-cache, must-revalidate");
        }
    }

    private void removeInformationHeaders(HttpHeaders headers) {
        HEADERS_TO_REMOVE.forEach(header -> {
            if (headers.containsKey(header)) {
                headers.remove(header);
                log.debug("Removed information disclosure header: {}", header);
            }
        });
    }

    private boolean isStaticResource(String path) {
        return STATIC_EXTENSIONS.stream().anyMatch(path::endsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
