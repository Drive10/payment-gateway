package dev.payment.api.auth;

import dev.payment.api.ApiTestBase;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthApiTest extends ApiTestBase {

    private static String accessToken;
    private static String refreshToken;
    private static String testEmail = "api-test-" + System.currentTimeMillis() + "@example.com";
    private static String testPassword = "TestPass123!";

    @Test
    @Order(1)
    @DisplayName("POST /auth/register - should register new user")
    void registerUser() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "API",
                        "lastName": "TestUser"
                    }
                    """.formatted(testEmail, testPassword))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("user.email", equalTo(testEmail))
                .body("user.firstName", equalTo("API"))
                .body("user.lastName", equalTo("TestUser"))
                .body("user.fullName", equalTo("API TestUser"))
                .body("user.roles", hasItem("USER"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/login - should login with valid credentials")
    void loginWithValidCredentials() {
        var response = given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "%s"
                    }
                    """.formatted(testEmail, testPassword))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("user.email", equalTo(testEmail))
                .body("user.fullName", equalTo("API TestUser"))
                .extract()
                .response();

        accessToken = response.path("accessToken");
        refreshToken = response.path("refreshToken");
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/login - should reject invalid password")
    void loginWithInvalidPassword() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "wrongpassword"
                    }
                    """.formatted(testEmail))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("error.code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/login - should reject non-existent email")
    void loginWithNonExistentEmail() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "nonexistent@example.com",
                        "password": "somepassword"
                    }
                    """)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("error.code", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /auth/register - should reject duplicate email")
    void registerDuplicateEmail() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "%s",
                        "password": "%s",
                        "firstName": "Duplicate",
                        "lastName": "User"
                    }
                    """.formatted(testEmail, testPassword))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(409);
    }

    @Test
    @Order(6)
    @DisplayName("POST /auth/register - should reject invalid email format")
    void registerInvalidEmail() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "not-an-email",
                        "password": "%s",
                        "firstName": "Bad",
                        "lastName": "Email"
                    }
                    """.formatted(testPassword))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /auth/register - should reject short password")
    void registerShortPassword() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "email": "short@example.com",
                        "password": "123",
                        "firstName": "Short",
                        "lastName": "Password"
                    }
                    """)
                .when()
                .post("/auth/register")
                .then()
                .statusCode(400)
                .body("error.code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /auth/refresh - should refresh valid token")
    void refreshToken() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "refreshToken": "%s"
                    }
                    """.formatted(refreshToken))
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("POST /auth/refresh - should reject invalid token")
    void refreshInvalidToken() {
        given().spec(spec)
                .contentType("application/json")
                .body("""
                    {
                        "refreshToken": "invalid.token.here"
                    }
                    """)
                .when()
                .post("/auth/refresh")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(10)
    @DisplayName("GET /auth/me - should return current user")
    void getCurrentUser() {
        given().spec(spec)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/auth/me")
                .then()
                .statusCode(200)
                .body("email", equalTo(testEmail))
                .body("fullName", equalTo("API TestUser"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /auth/me - should reject without token")
    void getCurrentUserWithoutToken() {
        given().spec(spec)
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(12)
    @DisplayName("GET /auth/me - should reject with invalid token")
    void getCurrentUserWithInvalidToken() {
        given().spec(spec)
                .header("Authorization", "Bearer invalid.token.here")
                .when()
                .get("/auth/me")
                .then()
                .statusCode(401);
    }
}
