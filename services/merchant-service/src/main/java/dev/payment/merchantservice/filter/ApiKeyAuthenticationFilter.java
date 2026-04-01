package dev.payment.merchantservice.filter;

import dev.payment.merchantservice.entity.ApiKey;
import dev.payment.merchantservice.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "Bearer ";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/merchants") && !path.equals("/api/v1/merchants")) {
            Optional<String> keyOpt = extractApiKey(request);
            if (keyOpt.isPresent()) {
                Optional<ApiKey> validatedKey = apiKeyService.validateApiKey(keyOpt.get());
                if (validatedKey.isPresent()) {
                    ApiKey apiKey = validatedKey.get();
                    request.setAttribute("apiKeyId", apiKey.getId());
                    request.setAttribute("merchantId", apiKey.getMerchantId());
                    request.setAttribute("apiKeyPermissions", apiKey.getPermissions());
                    
                    apiKeyService.recordKeyUsage(apiKey.getId(), request.getRemoteAddr());
                    log.debug("API key authenticated: merchantId={}, keyId={}", 
                            apiKey.getMerchantId(), apiKey.getId());
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid or expired API key\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractApiKey(HttpServletRequest request) {
        String header = request.getHeader(API_KEY_HEADER);
        if (header != null && header.startsWith(API_KEY_PREFIX)) {
            return Optional.of(header.substring(API_KEY_PREFIX.length()));
        }
        return Optional.empty();
    }
}
