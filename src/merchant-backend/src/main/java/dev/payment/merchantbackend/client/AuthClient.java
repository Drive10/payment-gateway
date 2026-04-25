package dev.payment.merchantbackend.client;

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
public class AuthClient {
    private final RestTemplate restTemplate;
    
    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    public String getApiKey(String merchantId, String jwt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = authServiceUrl + "/merchant/auth/get-api-key?merchantId=" + merchantId;
            log.info("Fetching API key from: {}", url);
            ResponseEntity<Map> response = 
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null) {
                        return (String) data.get("apiKey");
                    }
                }
                log.warn("Unexpected response format: {}", body);
            }
        } catch (Exception e) {
            log.error("Error fetching API key from auth-service: {} - {}", e.getMessage(), e.toString());
        }
        return null;
    }
}
