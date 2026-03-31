package dev.payment.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class WebhookRequest {
    
    @NotBlank(message = "URL is required")
    private String url;
    
    @NotBlank(message = "Event type is required")
    private String eventType;
    
    private Object payload;
    private String secret;
    private int maxRetries;
    private Map<String, String> headers;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
}
