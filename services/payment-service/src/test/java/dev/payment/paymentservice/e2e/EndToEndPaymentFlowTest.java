package dev.payment.paymentservice.e2e;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "(?i)true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndPaymentFlowTest {

  @LocalServerPort
  int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  @Order(1)
  @DisplayName("Phase 1: End-to-end path - Success path (map-based)")
  void endToEnd_Success_Path_MapBased() {
    // Build order payload as a Map
    Map<String, Object> orderPayload = new HashMap<>();
    orderPayload.put("merchantId", "00000000-0000-0000-0000-000000000001");
    orderPayload.put("amount", 100.0);
    orderPayload.put("currency", "USD");
    orderPayload.put("description", "E2E order from test");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String,Object>> orderEntity = new HttpEntity<>(orderPayload, headers);

    // Create order via gateway (assumes gateway routes to order-service)
    ResponseEntity<Map> orderResp = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/orders", orderEntity, Map.class);
    assertEquals(HttpStatus.OK, orderResp.getStatusCode());
    Map<String,Object> orderBody = orderResp.getBody();
    String orderId = null;
    if (orderBody != null) {
      orderId = getFirstString(orderBody, "id", "orderId");
    }
    assertNotNull(orderId, "Order response did not contain id/orderId");

    // Build payment payload
    Map<String,Object> paymentPayload = new HashMap<>();
    paymentPayload.put("orderId", orderId);
    paymentPayload.put("merchantId", "00000000-0000-0000-0000-000000000001");
    paymentPayload.put("amount", 100.0);
    paymentPayload.put("currency", "USD");
    paymentPayload.put("method", "CARD");
    paymentPayload.put("provider", "STRIPE");
    paymentPayload.put("transactionMode", "TEST");
    paymentPayload.put("notes", "E2E test payment");

    HttpEntity<Map<String,Object>> paymentEntity = new HttpEntity<>(paymentPayload, headers);

    ResponseEntity<Map> paymentResp = restTemplate.postForEntity("http://localhost:" + port + "/api/v1/payments", paymentEntity, Map.class);
    assertEquals(HttpStatus.OK, paymentResp.getStatusCode());
    Map<String,Object> paymentBody = paymentResp.getBody();
    String paymentId = getFirstString(paymentBody, "id", "paymentId");
    assertNotNull(paymentId, "Payment response did not contain id/paymentId");

    // Poll for status until terminal or timeout
    String status = getFirstString(paymentBody, "status", "paymentStatus");
    int tries = 0;
    while (status != null && !(status.equalsIgnoreCase("CAPTURED") || status.equalsIgnoreCase("COMPLETED") || status.equalsIgnoreCase("SUCCEEDED")) && tries < 60) {
      try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
      ResponseEntity<Map> r = restTemplate.getForEntity("http://localhost:" + port + "/api/v1/payments/" + paymentId, Map.class);
      if (r.getStatusCode() == HttpStatus.OK && r.getBody() != null) {
        status = getFirstString(r.getBody(), "status", "paymentStatus");
      } else {
        break;
      }
      tries++;
    }

    assertNotNull(status, "Payment status did not progress");
    assertTrue(status.equalsIgnoreCase("CAPTURED") || status.equalsIgnoreCase("COMPLETED"));
  }

  // Helpers
  private String getFirstString(Map<String,Object> map, String... keys){
    if (map == null) return null;
    for (String k : keys){
      Object v = map.get(k);
      if (v != null) return v.toString();
    }
    return null;
  }
}
