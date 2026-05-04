package dev.payment.paymentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String MERCHANT_ID_MDC_KEY = "merchantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                              FilterChain filterChain) throws ServletException, IOException {
        String correlationId = getOrGenerateCorrelationId(request);
        
        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
            MDC.remove(MERCHANT_ID_MDC_KEY);
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null && !correlationId.isBlank() ? correlationId : UUID.randomUUID().toString();
    }

    public static void setUserContext(String userId, String merchantId) {
        if (userId != null) MDC.put(USER_ID_MDC_KEY, userId);
        if (merchantId != null) MDC.put(MERCHANT_ID_MDC_KEY, merchantId);
    }
}