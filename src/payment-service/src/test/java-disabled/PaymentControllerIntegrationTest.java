package dev.payment.paymentservice.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.exception.GlobalPaymentExceptionHandler;
import dev.payment.paymentservice.payment.service.AuthService;
import dev.payment.paymentservice.payment.service.PaymentService;
import dev.payment.paymentservice.payment.controller.PaymentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("Payment API Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/payments")
    class CreatePayment {

        @Test
        @DisplayName("Should require Idempotency-Key header")
        @WithMockUser
        void requiresIdempotencyKey() throws Exception {
            CreatePaymentRequest request = new CreatePaymentRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    PaymentMethod.CARD,
                    "SIMULATOR",
                    dev.payment.paymentservice.payment.domain.enums.TransactionMode.TEST,
                    "Test payment"
            );

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MISSING_IDEMPOTENCY_KEY"));
        }

        @Test
        @DisplayName("Should accept valid payment request with idempotency key")
        @WithMockUser
        void acceptsValidRequest() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();
            CreatePaymentRequest request = new CreatePaymentRequest(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    PaymentMethod.CARD,
                    "SIMULATOR",
                    dev.payment.paymentservice.payment.domain.enums.TransactionMode.TEST,
                    "Test payment"
            );

            mockMvc.perform(post("/api/v1/payments")
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{id}")
    class GetPayment {

        @Test
        @DisplayName("Should return 404 for non-existent payment")
        @WithMockUser
        void returns404ForNonExistent() throws Exception {
            UUID paymentId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/capture")
    class CapturePayment {

        @Test
        @DisplayName("Should require authentication")
        void requiresAuth() throws Exception {
            mockMvc.perform(post("/api/v1/payments/{paymentId}/capture", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}