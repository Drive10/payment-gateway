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
public class PaytmClient {

    private final RestTemplate restTemplate;
    private final String baseUrl = "https://securegw-stage.paytm.in"; // placeholder staging
    private final String secret = System.getenv().getOrDefault("PAYTM_SECRET","");

    public PaytmClient(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public ResponseEntity<String> createOrder(String bodyJson) {
        // TODO: Construct checksum header and POST to Paytm order API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("CHECKSUMHASH", sign(bodyJson));
        return restTemplate.exchange(baseUrl + "/the/order/api", HttpMethod.POST, new HttpEntity<>(bodyJson, headers), String.class);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
        } catch (Exception e) { return ""; }
    }
}
