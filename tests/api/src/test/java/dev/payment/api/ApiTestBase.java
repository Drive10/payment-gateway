package dev.payment.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiTestBase {

    protected static RequestSpecification spec;

    @BeforeAll
    void setUp() {
        String baseUrl = System.getProperty("api.base.url", "http://localhost:8080");
        
        spec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setBasePath("/api/v1")
                .addFilter(new RequestLoggingFilter())
                .addFilter(new ResponseLoggingFilter())
                .build();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
