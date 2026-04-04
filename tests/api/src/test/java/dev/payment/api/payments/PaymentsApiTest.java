package dev.payment.api.payments;

import dev.payment.api.ApiTestBase;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentsApiTest extends ApiTestBase {

    private static String accessToken;
    private static String paymentId;
    private static String orderId;
    private static final String EMAIL = "payment-test-" + System.currentTimeMillis() + "@example.com";
    private static final String PASSWORD = "PaymentTest123!";

    @BeforeAll
    void setup() {
        var authResponse = given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "Payment",
                        "lastName": "Tester"
                    }
                    """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();

        accessToken = authResponse.path("accessToken");

        var orderResponse = given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 10000,
                        "currency": "USD",
                        "customerEmail": "payment-customer@example.com",
                        "description": "Order for payment test"
                    }
                    """)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .response();

        orderId = orderResponse.path("id");
    }

    @Test
    @Order(1)
    @DisplayName("POST /payments - should create payment")
    void createPayment() {
        String idempotencyKey = UUID.randomUUID().toString();

        var response = given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                    {
                        "orderId": "%s",
                        "amount": 10000,
                        "currency": "USD",
                        "provider": "STRIPE",
                        "paymentMethod": "CARD"
                    }
                    """.formatted(orderId))
                .when()
                .post("/payments")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("orderId", equalTo(orderId))
                .body("amount", equalTo(10000))
                .body("currency", equalTo("USD"))
                .body("status", notNullValue())
                .extract()
                .response();

        paymentId = response.path("id");
    }

    @Test
    @Order(2)
    @DisplayName("POST /payments - should reject duplicate idempotency key")
    void createPaymentDuplicateIdempotency() {
        String idempotencyKey = UUID.randomUUID().toString();

        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                    {
                        "orderId": "%s",
                        "amount": 10000,
                        "currency": "USD",
                        "provider": "STRIPE",
                        "paymentMethod": "CARD"
                    }
                    """.formatted(orderId))
                .when()
                .post("/payments")
                .then()
                .statusCode(200);

        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                    {
                        "orderId": "%s",
                        "amount": 10000,
                        "currency": "USD",
                        "provider": "STRIPE",
                        "paymentMethod": "CARD"
                    }
                    """.formatted(orderId))
                .when()
                .post("/payments")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    @DisplayName("GET /payments/{id} - should retrieve payment by ID")
    void getPaymentById() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/payments/" + paymentId)
                .then()
                .statusCode(200)
                .body("id", equalTo(paymentId))
                .body("orderId", equalTo(orderId))
                .body("amount", equalTo(10000));
    }

    @Test
    @Order(4)
    @DisplayName("POST /payments/{id}/capture - should capture payment")
    void capturePayment() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 10000
                    }
                    """)
                .when()
                .post("/payments/" + paymentId + "/capture")
                .then()
                .statusCode(200)
                .body("status", equalTo("CAPTURED"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /payments/{id}/capture - should reject double capture")
    void doubleCapture() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 10000
                    }
                    """)
                .when()
                .post("/payments/" + paymentId + "/capture")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("POST /payments/{id}/refund - should refund payment")
    void refundPayment() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 10000,
                        "reason": "Customer requested refund"
                    }
                    """)
                .when()
                .post("/payments/" + paymentId + "/refund")
                .then()
                .statusCode(200)
                .body("status", equalTo("REFUNDED"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /payments - should list payments")
    void listPayments() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/payments")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("POST /payments - should reject without auth")
    void createPaymentWithoutAuth() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "orderId": "%s",
                        "amount": 1000,
                        "currency": "USD",
                        "provider": "STRIPE",
                        "paymentMethod": "CARD"
                    }
                    """.formatted(orderId))
                .when()
                .post("/payments")
                .then()
                .statusCode(401);
    }
}
