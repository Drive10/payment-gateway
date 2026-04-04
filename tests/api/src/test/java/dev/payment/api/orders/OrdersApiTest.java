package dev.payment.api.orders;

import dev.payment.api.ApiTestBase;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrdersApiTest extends ApiTestBase {

    private static String accessToken;
    private static String orderId;
    private static final String EMAIL = "order-test-" + System.currentTimeMillis() + "@example.com";
    private static final String PASSWORD = "OrderTest123!";

    @BeforeAll
    void setup() {
        var loginResponse = given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "Order",
                        "lastName": "Tester"
                    }
                    """.formatted(EMAIL, PASSWORD))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();

        accessToken = loginResponse.path("accessToken");
    }

    @Test
    @Order(1)
    @DisplayName("POST /orders - should create order")
    void createOrder() {
        var response = given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 5000,
                        "currency": "USD",
                        "customerEmail": "customer@example.com",
                        "description": "Test order from API tests"
                    }
                    """)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("amount", equalTo(5000))
                .body("currency", equalTo("USD"))
                .body("status", equalTo("PENDING"))
                .extract()
                .response();

        orderId = response.path("id");
    }

    @Test
    @Order(2)
    @DisplayName("GET /orders/{id} - should retrieve order by ID")
    void getOrderById() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/orders/" + orderId)
                .then()
                .statusCode(200)
                .body("id", equalTo(orderId))
                .body("amount", equalTo(5000))
                .body("currency", equalTo("USD"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /orders - should list orders")
    void listOrders() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/orders")
                .then()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("POST /orders - should reject without auth")
    void createOrderWithoutAuth() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "amount": 1000,
                        "currency": "USD",
                        "customerEmail": "test@example.com",
                        "description": "Should fail"
                    }
                    """)
                .when()
                .post("/orders")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    @DisplayName("POST /orders - should reject invalid amount")
    void createOrderWithInvalidAmount() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("""
                    {
                        "amount": -100,
                        "currency": "USD",
                        "customerEmail": "test@example.com",
                        "description": "Negative amount"
                    }
                    """)
                .when()
                .post("/orders")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("GET /orders/{id} - should return 404 for non-existent order")
    void getOrderNotFound() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/orders/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
