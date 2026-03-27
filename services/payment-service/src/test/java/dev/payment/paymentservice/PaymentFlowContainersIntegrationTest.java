package dev.payment.paymentservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import dev.payment.paymentservice.integration.processor.PaymentProcessorCaptureResponse;
import dev.payment.paymentservice.integration.processor.PaymentProcessorClient;
import dev.payment.paymentservice.integration.processor.PaymentProcessorIntentResponse;
import dev.payment.paymentservice.repository.RoleRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PaymentFlowContainersIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("paymentdb")
            .withUsername("payment")
            .withPassword("paymentpass");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("application.security.jwt.secret-key", () -> "VGhpc0lzQVN0cm9uZ0Jhc2U2NEVuY29kZWRKU1dUU2VjcmV0S2V5Rm9yRmluVGVjaEFQSUM=");
        registry.add("application.kafka.topic.payment-events", () -> "payment.events");
        registry.add("application.payment.checkout-base-url", () -> "http://localhost:3000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private PaymentProcessorClient paymentProcessorClient;

    private static KafkaConsumer<String, PaymentEventMessage> consumer;

    @BeforeEach
    void setUp() {
        createRoleIfMissing(RoleName.ADMIN);
        createRoleIfMissing(RoleName.USER);

        Mockito.when(paymentProcessorClient.createIntent(Mockito.any(), Mockito.anyString(), Mockito.any()))
                .thenAnswer(invocation -> new PaymentProcessorIntentResponse(
                        "tc_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
                        "https://checkout.test/" + UUID.randomUUID(),
                        false
                ));
        Mockito.when(paymentProcessorClient.capture(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> new PaymentProcessorCaptureResponse(
                        "tc_pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
                        "tc_sig_" + UUID.randomUUID().toString().substring(0, 12),
                        "tc_txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18),
                        false
                ));

        if (consumer == null) {
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-testcontainers");
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
            properties.put(JsonDeserializer.TRUSTED_PACKAGES, "dev.payment.common.events");
            properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentEventMessage.class.getName());
            consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(List.of("payment.events"));
        }
    }

    @AfterAll
    static void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void refundFlowShouldPublishKafkaEvents() throws Exception {
        String email = "containers+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Container User","email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = readField(loginResponse, "/data/accessToken");

        String orderResponse = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference":"tc-order-1001","amount":4999.00,"currency":"INR","description":"tc"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String orderId = readField(orderResponse, "/data/id");

        String paymentResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "tc-payment-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","method":"CARD","provider":"RAZORPAY_PRIMARY","transactionMode":"PRODUCTION","notes":"tc"}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paymentId = readField(paymentResponse, "/data/id");

        mockMvc.perform(post("/api/v1/payments/{paymentId}/capture", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/refunds", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "tc-refund-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1499.00,"reason":"container refund"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_REFUNDED"));

        List<String> eventTypes = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && eventTypes.size() < 3) {
            ConsumerRecords<String, PaymentEventMessage> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, PaymentEventMessage> record : records) {
                if (record.value() != null && paymentId.equals(record.value().paymentId().toString())) {
                    eventTypes.add(record.value().eventType());
                }
            }
        }

        assertThat(eventTypes).contains("payment.created", "payment.captured", "payment.refunded");
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
}
