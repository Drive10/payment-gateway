package dev.payflow.onboarding.dto;

public class AuthResponse {
    private String token;
    private String apiKey;
    private String email;
    private String merchantName;

    public AuthResponse() {}
    public AuthResponse(String token, String apiKey, String email, String merchantName) {
        this.token = token;
        this.apiKey = apiKey;
        this.email = email;
        this.merchantName = merchantName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
}
