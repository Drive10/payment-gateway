package dev.payment.paymentservice.client;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@Profile("prod")
public class PhonePeClient {

    private final RestTemplate restTemplate;
    private final String baseUrl = "https://api.phonepe.com/apis/hermes"; // placeholder
    private final String secret = System.getenv().getOrDefault("PHONEPE_SECRET","");

    public PhonePeClient(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public ResponseEntity<String> createCollect(String bodyJson) {
        // TODO: Construct headers/signature as per PhonePe docs and POST
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-VERIFY", sign(bodyJson));
        return restTemplate.exchange(baseUrl + "/v1/pay", HttpMethod.POST, new HttpEntity<>(bodyJson, headers), String.class);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
        } catch (Exception e) { return ""; }
    }
}
