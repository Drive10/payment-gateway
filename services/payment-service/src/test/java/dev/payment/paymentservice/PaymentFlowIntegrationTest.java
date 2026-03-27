package dev.payment.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import dev.payment.paymentservice.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSupportConfig.class)
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PaymentOutboxEventRepository paymentOutboxEventRepository;

    @BeforeEach
    void ensureRoles() {
        createRoleIfMissing(RoleName.ADMIN);
        createRoleIfMissing(RoleName.USER);
    }

    @Test
    void paymentFlowShouldRegisterLoginCreateAndCapturePayment() throws Exception {
        String email = "flow+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Flow User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = readField(loginResponse, "/data/accessToken");
        String refreshToken = readField(loginResponse, "/data/refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "checkout-test-1001",
                                  "amount": 2499.00,
                                  "currency": "INR",
                                  "description": "Integration test payment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = readField(orderResponse, "/data/id");

        String paymentResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "integration-payment-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "method": "UPI",
                                  "provider": "razorpay_simulator",
                                  "transactionMode": "TEST",
                                  "notes": "integration test"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.transactionMode").value("TEST"))
                .andExpect(jsonPath("$.data.simulated").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentId = readField(paymentResponse, "/data/id");

        mockMvc.perform(post("/api/v1/payments/{paymentId}/capture", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerPaymentId": "pay_sim_test_001",
                                  "providerSignature": "simulated_signature_hash"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.transactionMode").value("TEST"))
                .andExpect(jsonPath("$.data.simulated").value(true));
    }

    @Test
    void adminEndpointShouldRejectRegularUser() throws Exception {
        String email = "user+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Regular User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = readField(loginResponse, "/data/accessToken");

        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void refundApiShouldBeIdempotentAndPreventDuplicateFinancialImpact() throws Exception {
        Session session = createCapturedPaymentSession();

        String firstRefund = mockMvc.perform(post("/api/v1/payments/{paymentId}/refunds", session.paymentId())
                        .header("Authorization", "Bearer " + session.token())
                        .header("Idempotency-Key", "refund-key-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 499.00,
                                  "reason": "customer_request"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(499.00))
                .andExpect(jsonPath("$.data.refundedAmount").value(499.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondRefund = mockMvc.perform(post("/api/v1/payments/{paymentId}/refunds", session.paymentId())
                        .header("Authorization", "Bearer " + session.token())
                        .header("Idempotency-Key", "refund-key-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 499.00,
                                  "reason": "customer_request"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundedAmount").value(499.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(readField(secondRefund, "/data/refundReference"))
                .isEqualTo(readField(firstRefund, "/data/refundReference"));
    }

    @Test
    void paymentCreateShouldReplayStoredResponseForDuplicateIdempotencyKey() throws Exception {
        String email = "idempotent+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Idempotent User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = readField(loginResponse, "/data/accessToken");

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "idem-order-%d",
                                  "amount": 1499.00,
                                  "currency": "INR",
                                  "description": "Idempotent payment"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = readField(orderResponse, "/data/id");
        long outboxBefore = paymentOutboxEventRepository.count();

        String firstPayment = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-payment-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "method": "UPI",
                                  "provider": "razorpay_simulator",
                                  "transactionMode": "TEST",
                                  "notes": "first attempt"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String duplicatePayment = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-payment-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "method": "UPI",
                                  "provider": "razorpay_simulator",
                                  "transactionMode": "TEST",
                                  "notes": "first attempt"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(readField(duplicatePayment, "/data/id")).isEqualTo(readField(firstPayment, "/data/id"));
        assertThat(paymentOutboxEventRepository.count()).isEqualTo(outboxBefore + 1);
    }

    @Test
    void webhookShouldRejectInvalidSignatureAndIgnoreReplay() throws Exception {
        Session session = createCapturedPaymentSession();

        String payload = """
                {
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_test_001",
                        "payment_id": "%s",
                        "amount": 250.00,
                        "notes": "webhook_refund"
                      }
                    }
                  }
                }
                """.formatted(session.providerPaymentId());

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_bad_sig_001")
                        .header("X-Razorpay-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        String signature = sign(payload, "test_webhook_secret");

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_good_sig_001")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Event-Id", "evt_good_sig_001")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", session.paymentId())
                        .header("Authorization", "Bearer " + session.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundedAmount").value(250.00))
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_REFUNDED"));
    }

    private void createRoleIfMissing(RoleName roleName) {
        roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            return roleRepository.save(role);
        });
    }

    private String readField(String json, String pointer) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode node = root.at(pointer);
        assertThat(node.isMissingNode()).isFalse();
        return node.asText();
    }

    private Session createCapturedPaymentSession() throws Exception {
        String email = "session+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Session User",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = readField(loginResponse, "/data/accessToken");

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "session-order-%d",
                                  "amount": 2499.00,
                                  "currency": "INR",
                                  "description": "Session payment"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = readField(orderResponse, "/data/id");

        String paymentResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "session-payment-" + System.currentTimeMillis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "method": "UPI",
                                  "provider": "razorpay_simulator",
                                  "transactionMode": "TEST",
                                  "notes": "session test"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentId = readField(paymentResponse, "/data/id");

        String providerPaymentId = "pay_sim_test_" + System.currentTimeMillis();

        mockMvc.perform(post("/api/v1/payments/{paymentId}/capture", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerPaymentId": "%s",
                                  "providerSignature": "simulated_signature_hash"
                                }
                                """.formatted(providerPaymentId)))
                .andExpect(status().isOk());

        return new Session(token, paymentId, providerPaymentId);
    }

    private String sign(String payload, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private record Session(String token, String paymentId, String providerPaymentId) {
    }
}
