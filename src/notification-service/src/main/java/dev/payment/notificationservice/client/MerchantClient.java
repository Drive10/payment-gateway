package dev.payment.notificationservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Slf4j
@ConfigurationProperties(prefix = "services.auth-service")
public class MerchantClient {
    private final RestTemplate restTemplate;
    private String url;

    public MerchantClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMerchantWebhookUrl(String merchantId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            String url = this.url + "/merchant/auth/details?merchantId=" + merchantId;
            ResponseEntity<Map> response = 
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null) {
                    return (String) data.get("webhookUrl");
                }
            }
        } catch (Exception e) {
            log.error("Error fetching merchant details from auth-service: {}", e.getMessage());
        }
        return null;
    }
}