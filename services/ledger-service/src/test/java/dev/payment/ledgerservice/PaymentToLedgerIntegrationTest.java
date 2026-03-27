package dev.payment.ledgerservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.ledgerservice.domain.AccountType;
import dev.payment.ledgerservice.dto.request.CreateAccountRequest;
import dev.payment.ledgerservice.repository.JournalEntryRepository;
import dev.payment.ledgerservice.service.LedgerService;
import dev.payment.paymentservice.domain.Role;
import dev.payment.paymentservice.domain.enums.RoleName;
import dev.payment.paymentservice.repository.RoleRepository;
import dev.payment.paymentservice.service.PaymentOutboxRelay;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class PaymentToLedgerIntegrationTest {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static ConfigurableApplicationContext paymentContext;
    private static ConfigurableApplicationContext ledgerContext;
    private static MockMvc paymentMvc;
    private static ObjectMapper objectMapper;

    private RoleRepository roleRepository;
    private PaymentOutboxRelay paymentOutboxRelay;
    private JournalEntryRepository journalEntryRepository;

    @BeforeAll
    static void startApplications() {
        String kafkaBootstrap = KAFKA.getBootstrapServers();
        String topic = "payment.events.cross";

        paymentContext = new SpringApplicationBuilder(dev.payment.paymentservice.Application.class)
                .profiles("test")
                .properties(
                        "server.port=0",
                        "spring.kafka.bootstrap-servers=" + kafkaBootstrap,
                        "spring.datasource.url=jdbc:h2:mem:payment-cross-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "spring.flyway.enabled=false",
                        "application.gateway-trust.enabled=false",
                        "application.reconciliation.enabled=false",
                        "application.kafka.topic.payment-events=" + topic,
                        "application.security.jwt.secret-key=VGhpc0lzQVN0cm9uZ0Jhc2U2NEVuY29kZWRKU1dUU2VjcmV0S2V5Rm9yRmluVGVjaEFQSUM="
                )
                .run();

        WebApplicationContext webApplicationContext = paymentContext.getBean(WebApplicationContext.class);
        paymentMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = paymentContext.getBean(ObjectMapper.class);

        ledgerContext = new SpringApplicationBuilder(dev.payment.ledgerservice.Application.class)
                .properties(
                        "server.port=0",
                        "spring.kafka.bootstrap-servers=" + kafkaBootstrap,
                        "spring.datasource.url=jdbc:h2:mem:ledger-cross-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.jpa.hibernate.ddl-auto=create-drop",
                        "spring.flyway.enabled=false",
                        "application.gateway-trust.enabled=false",
                        "application.kafka.topic.payment-events=" + topic
                )
                .run();

        LedgerService ledgerService = ledgerContext.getBean(LedgerService.class);
        ledgerService.createAccount(
                new CreateAccountRequest("CASH_ASSET", "Cash Asset", AccountType.ASSET));
        ledgerService.createAccount(
                new CreateAccountRequest(
                        "CUSTOMER_FUNDS_LIABILITY",
                        "Customer Funds",
                        AccountType.LIABILITY));
    }

    @AfterAll
    static void stopApplications() {
        if (paymentContext != null) {
            paymentContext.close();
        }
        if (ledgerContext != null) {
            ledgerContext.close();
        }
    }

    @BeforeEach
    void setUp() {
        roleRepository = paymentContext.getBean(RoleRepository.class);
        paymentOutboxRelay = paymentContext.getBean(PaymentOutboxRelay.class);
        journalEntryRepository = ledgerContext.getBean(JournalEntryRepository.class);
        journalEntryRepository.deleteAll();
        createRoleIfMissing(RoleName.ADMIN);
        createRoleIfMissing(RoleName.USER);
    }

    @Test
    void captureAndRefundShouldCreateLedgerJournalsThroughKafka() throws Exception {
        String email = "cross+" + System.currentTimeMillis() + "@example.com";
        String password = "User12345";

        paymentMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"fullName":"Cross Service User","email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk());

        String loginResponse = paymentMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = readField(loginResponse, "/data/accessToken");

        String orderResponse = paymentMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"externalReference":"cross-order-%d","amount":2499.00,"currency":"INR","description":"Cross service journal test"}
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String orderId = readField(orderResponse, "/data/id");

        String paymentResponse = paymentMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "cross-pay-" + System.currentTimeMillis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"orderId":"%s","method":"UPI","provider":"RAZORPAY_SIMULATOR","transactionMode":"TEST","notes":"cross-service"}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String paymentId = readField(paymentResponse, "/data/id");

        paymentMvc.perform(post("/api/v1/payments/{paymentId}/capture", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"));

        paymentOutboxRelay.relayPendingEvents();

        String refundResponse = paymentMvc.perform(post("/api/v1/payments/{paymentId}/refunds", paymentId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "cross-refund-" + System.currentTimeMillis())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"amount":499.00,"reason":"customer_request"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_REFUNDED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        paymentOutboxRelay.relayPendingEvents();

        String refundReference = readField(refundResponse, "/data/refundReference");
        awaitJournalCount(2);

        List<String> references = journalEntryRepository.findAll().stream()
                .map(entry -> entry.getReference())
                .toList();
        assertThat(references).contains("capture:" + paymentId, "refund:" + refundReference);
    }

    private void awaitJournalCount(int expectedCount) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            if (journalEntryRepository.count() >= expectedCount) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(journalEntryRepository.count()).isGreaterThanOrEqualTo(expectedCount);
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
