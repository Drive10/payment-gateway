package dev.payment.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import dev.payment.paymentservice.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
@Import(PaymentFlowIntegrationTest.NoOpEventPublisherConfig.class)
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

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

    @TestConfiguration
    static class NoOpEventPublisherConfig {

        @Bean
        @Primary
        dev.payment.paymentservice.service.PaymentEventPublisher paymentEventPublisher() {
            return new dev.payment.paymentservice.service.PaymentEventPublisher(null, "test-topic") {
                @Override
                public void publish(String eventType, dev.payment.paymentservice.domain.Payment payment, java.util.Map<String, String> metadata) {
                }
            };
        }
    }
}
