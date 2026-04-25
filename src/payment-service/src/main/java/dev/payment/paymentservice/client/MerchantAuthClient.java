package dev.payment.paymentservice.client;

import dev.payment.paymentservice.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantAuthClient {
    private final RestTemplate restTemplate;
    
    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    public Map<String, Object> validateApiKey(String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ApiResponse> response = 
                restTemplate.exchange(authServiceUrl + "/merchant/auth/validate-key", HttpMethod.GET, entity, ApiResponse.class);

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return (Map<String, Object>) response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Error validating API key via auth-service: {}", e.getMessage());
        }
        return null;
    }
}
