package dev.payflow.contracts;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactRunner;
import au.com.dius.pact.consumer.junit Verification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(PactRunner.class)
public class PaymentServiceContractTest {

    @Pact(consumer = "api-gateway", provider = "payment-service")
    public RequestResponsePact createPaymentContract(PactDslWithProvider builder) {
        return builder
            .uponReceiving("a request to create a payment")
                .path("/payments")
                .method("POST")
                .headers(
                    "Content-Type", "application/json",
                    "Authorization", "Bearer valid-token",
                    "Idempotency-Key", "idem-123"
                )
                .body(new PactDslJsonBody()
                    .stringType("orderId", "order_123")
                    .stringType("merchantId", "550e8400-e29b-41d4-a716-446655440000")
                    .stringValue("paymentMethod", "CARD")
                    .stringValue("provider", "STRIPE")
                    .stringValue("transactionMode", "TEST")
                )
            .willRespondWith()
                .status(201)
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                    .booleanType("success", true)
                    .object("data", data -> data
                        .stringType("id", "550e8400-e29b-41d4-a716-446655440001")
                        .stringValue("status", "CREATED")
                        .stringType("checkoutUrl", "https://checkout.payflow.dev/pay/abc123")
                    )
                )
            .toPact();
    }

    @Pact(consumer = "dashboard-service", provider = "payment-service")
    public RequestResponsePact getPaymentsContract(PactDslWithProvider builder) {
        return builder
            .uponReceiving("a request to get all payments")
                .path("/payments")
                .method("GET")
                .headers(
                    "Authorization", "Bearer valid-token"
                )
                .query("page=0&size=10")
            .willRespondWith()
                .status(200)
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                    .booleanType("success", true)
                    .array("data", arr -> arr
                        .object(item -> item
                            .stringType("id", "550e8400-e29b-41d4-a716-446655440001")
                            .stringValue("status", "CAPTURED")
                        )
                    )
                )
            .toPact();
    }

    @Pact(consumer = "api-gateway", provider = "payment-service")
    public RequestResponsePact createPaymentLinkContract(PactDslWithProvider builder) {
        return builder
            .uponReceiving("a request to create payment link")
                .path("/payments/links")
                .method("POST")
                .headers(
                    "Content-Type", "application/json",
                    "Authorization", "Bearer valid-token"
                )
                .body(new PactDslJsonBody()
                    .minArrayLike("amount", 1, 100.00, 1000.00)
                    .stringValue("currency", "INR")
                )
            .willRespondWith()
                .status(201)
                .headers("Content-Type", "application/json")
                .body(new PactDslJsonBody()
                    .booleanType("success", true)
                    .object("data", data -> data
                        .stringType("id", "link_123")
                        .stringType("checkoutUrl", "https://payflow.dev/pay/link_123")
                    )
                )
            .toPact();
    }

    @Verification
    @Test
    public void testCreatePaymentContract() {
        given()
            .contentType("application/json")
            .header("Authorization", "Bearer valid-token")
            .header("Idempotency-Key", "idem-123")
            .body("""
                {
                    "orderId": "order_123",
                    "merchantId": "550e8400-e29b-41d4-a716-446655440000",
                    "paymentMethod": "CARD",
                    "provider": "STRIPE",
                    "transactionMode": "TEST"
                }
                """)
        .when()
            .post("http://localhost:8083/payments")
        .then()
            .statusCode(201)
            .body("success", is(true))
            .body("data.id", is(notNullValue()));
    }
}