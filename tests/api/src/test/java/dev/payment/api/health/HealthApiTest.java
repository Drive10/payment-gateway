package dev.payment.api.health;

import dev.payment.api.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class HealthApiTest extends ApiTestBase {

    @Test
    @DisplayName("GET /actuator/health - should return healthy")
    void gatewayHealth() {
        given().spec(spec)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/health - should return auth service health")
    void authServiceHealth() {
        given().spec(spec)
                .when()
                .get("/auth/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/v1/orders/actuator/health - should return order service health")
    void orderServiceHealth() {
        given().spec(spec)
                .when()
                .get("/orders/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /api/v1/payments/actuator/health - should return payment service health")
    void paymentServiceHealth() {
        given().spec(spec)
                .when()
                .get("/payments/actuator/health")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /nonexistent - should return 404")
    void nonexistentEndpoint() {
        given().spec(spec)
                .when()
                .get("/nonexistent")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("OPTIONS /api/v1 - should support CORS preflight")
    void corsPreflight() {
        given().spec(spec)
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options("/auth/login")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", notNullValue());
    }
}
